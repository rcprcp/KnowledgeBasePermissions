package com.cottagecoders;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.cottagecoders.simpleslack.FetchMembers;
import com.cottagecoders.simpleslack.SendSlackMessage;
import com.cottagecoders.simpleslack.userlist.Member;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zendesk.client.v2.Zendesk;
import org.zendesk.client.v2.model.Status;
import org.zendesk.client.v2.model.Ticket;
import org.zendesk.client.v2.model.hc.Article;
import org.zendesk.client.v2.model.hc.UserSegment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KnowledgeBasePermissions {
  private static final Logger LOG = LogManager.getLogger(KnowledgeBasePermissions.class);
  private static final String SLACK_USER_NAMME = "bobp";
  @Parameter(names = {"--current"}, description = "current permission level")
  private String current = "";
  @Parameter(names = {"--destination"}, description = "destination permission level")
  private String destination = "";
  @Parameter(names = {"--printUserSegments"}, description = "print a list of the UserSegment identifiers")
  private boolean printUsers = false;

  public static void main(String[] argv) {
    KnowledgeBasePermissions zdr = new KnowledgeBasePermissions();
    // parse starting args
    JCommander.newBuilder().addObject(zdr).build().parse(argv);
    zdr.run();
  }

  void run() {


    try (Zendesk zd = connectToZendesk()) {
      StringBuilder slackMessage = new StringBuilder(2000);
      slackMessage.append("KnowledgeBasePermissions  ");
      slackMessage.append(new Date());
      slackMessage.append("\n");

      Map<String, Long> segmentsByName = new HashMap<>();
      Map<Long, String> segmentsById = new HashMap<>();

      Iterable<UserSegment> userSegments = zd.getUserSegments();
      for (UserSegment u : userSegments) {
        segmentsByName.put(u.getName(), u.getId());
        segmentsById.put(u.getId(), u.getName());
      }

      // add "Everyone" because it does not shoe up in the UserSegment list.
      segmentsByName.put("Everyone", -1L);
      segmentsById.put(-1L, "Everyone");

      if (printUsers) {
        printUserSegments(segmentsByName);
        zd.close();
        System.exit(0);

      }

      // validate user input... need to have a current and a destination
      if (StringUtils.isEmpty(current) || StringUtils.isEmpty(destination) || current.equals(destination)) {
        errorMsg("invalid input, must have current and destination.", slackMessage);

      } else if (segmentsByName.get(current) == null) {
        errorMsg("invalid value for 'current'", slackMessage);

      } else if (segmentsByName.get(destination) == null) {
        errorMsg("invalid value for 'destination'", slackMessage);

      }
      int count = 0;
      Iterable<Article> articles = zd.getArticles();
      for (Article a : articles) {

        // kludge for null values in the UserSegment.  this apparently means "Everyone".
        if (a.getUserSegmentId() == null) {
          a.setUserSegmentId(-1L);   //set remapped value.
        }

        // does the article's UserSegment match the 'current' one we're looking for?
        if (segmentsById.get(a.getUserSegmentId()).equals(current)) {
          // matched by name, check kludge because "Everyone" was represented by null.
          Long dval = segmentsByName.get(destination);
          if (dval == -1) {             // this was the re-mapped value for null.
            a.setUserSegmentId(null);   // set null value occurs in the UserSegment in this article.
          } else {
            a.setUserSegmentId(dval);   // set new value in article.
          }

          zd.updateArticle(a);  // update the article.

          System.out.println("updated " + a.getTitle());
          ++count;
        }
      }
      System.out.println(count + " articles updated.");
      slackMessage.append(count);
      slackMessage.append(" articles updated.");
      SlackIt(slackMessage.toString());

    } catch (Exception ex) {
      System.out.println("exception: " + ex.getMessage());
      ex.printStackTrace();
      System.exit(5);
    }
  }

  void printUserSegments(Map<String, Long> segments) {
    for (String s : segments.keySet()) {
      System.out.println("u.getname() " + s + " u.getId() " + segments.get(s));
    }
  }

  void errorMsg(String param, StringBuilder sb) {
    String msg = String.format("Error: %s", param);
    System.out.println(msg);
    sb.append(msg);

    System.exit(1);
  }

  private Zendesk connectToZendesk() {
    return new Zendesk.Builder(System.getenv("ZENDESK_URL")).setUsername(System.getenv("ZENDESK_EMAIL")).setToken(System.getenv("ZENDESK_TOKEN")) // or .setPassword("...")
                   .build();
  }

  ArrayList<Ticket> getArticles(Zendesk zd, Status status, long orgId) {
    ArrayList<Ticket> results = new ArrayList<>();


    return results;
  }

  void SlackIt(String msg) {

    // fetch and parse the notification list.
    String envVar = System.getenv("SLACK_NOTIFICATION_LIST");
    if(StringUtils.isEmpty(envVar)) {
      System.out.println("No notifications - SLACK_NOTIFICATION_LIST is empty.");
      return;
    }

    String [] slackDisplayNames = envVar.split(",");
    if(slackDisplayNames == null || slackDisplayNames.length == 0) {
      System.out.println("No notifications - can't split SLACK_NOTIFICATION_LIST.");
      return;
    }

    try {
      FetchMembers fetch = new FetchMembers();

      Map<String, Member> members = null;
      // gather list of members from Slack
      members = fetch.fetchAllMembers();

      for(String s: slackDisplayNames) {
        // get specific user
        Member member = members.get(s.trim());
        if (member == null) {
          // user lookup failed.
          continue;
        }

        SendSlackMessage ssm = new SendSlackMessage();
        ssm.sendDM(member.getId(), msg);
      }

    } catch (IOException | InterruptedException ex) {
      System.out.println("fetch.fetchAllMembers() exception: " + ex.getMessage());
      ex.printStackTrace();
      System.exit(12);
    }
  }
}

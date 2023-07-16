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
import org.zendesk.client.v2.model.hc.PermissionGroup;
import org.zendesk.client.v2.model.hc.UserSegment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class KnowledgeBasePermissions {
  private static final Logger LOG = LogManager.getLogger(KnowledgeBasePermissions.class);

  @Parameter(names = {"--current"}, description = "current permission level")
  private String current = "";

  @Parameter(names = {"--destination"}, description = "destination permission level")
  private String destination = "";

  @Parameter(names = {"--printManagers"}, description = "print a list of 'managed by' identifiers")
  private boolean printManagers = false;

  @Parameter(names = {"--printUsers"}, description = "print a list of the User Segments identifiers")
  private boolean printUsers = false;

  private static final String SLACK_USER_NAMME = "bobp";

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

      // add "Everyone" because it does not sho up in the UserSegment list.
      segmentsByName.put("Everyone", -1L);
      segmentsById.put(-1L, "Everyone");

      if (printUsers) {
        printUserSegments(segmentsByName);
        zd.close();
        System.exit(0);

      }

      //TODO: remove debugging code.
      printUserSegments(segmentsByName);

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
          if (dval == -1) {  // this was the re-mapped value for null.
            a.setUserSegmentId(null);   // set null value in article.
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

    FetchMembers fetch = new FetchMembers();
    Map<String, Member> members = null;

    try {
      String SLACKLIB_TOKEN = System.getenv("SLACKLIB_TOKEN");
      members = fetch.fetchAllMembers();
      Member member = members.get("bobp");

      SendSlackMessage ssm = new SendSlackMessage();
      ssm.sendDM(member.getId(), msg);

    } catch (IOException | InterruptedException ex) {
      System.out.println("fetch.fetchAllMembers() exception: " + ex.getMessage());
      ex.printStackTrace();
      System.exit(12);
    }

  }

}


# Update Zendesk Knowledge Base Permissions

This program can update the UserSegment field on Zendesk Knowledge Base Articles. UserSegment controls who can see the Knowledge Base Articles.

To view the possible values for the UserSegment use this command:
```shell
java -jar KnowledgeBasePermissions-1.0-SNAPSHOT-jar-with-dependencies.jar --printUsers
```
These levels provide access to different groups of customers in Zendesk.  In the case I have seen, we were going to switch from users who were logged in to the support portal to everyone.
## Run the program you can use this sort of command:
```shell
java -jar KnowledgeBasePermissions-1.0-SNAPSHOT-jar-with-dependencies.jar --current "Signed-in users" --destination "Everyone"
```
In this code, "Everyone" is implemented as a special case, apparently Zendesk does not list "Everyone" when fetching the UserSegments.  
In fact, the UserSegment value for "Everyone" is actually a null, stored in a Long value.

## Environment
This program requires some environment variables to establish a connection to Zendesk and (optionally) to Slack: 
```shell
export ZENDESK_EMAIL="your.email@somewhere.com"
export ZENDESK_TOKEN="ffbglkfbYourZendeskTokenptrhb5jp42m"
export ZENDESK_URL="https://somewhere.zendesk.com"
export SLACKLIB_TOKEN="xoxb-34859038409-45908608430423-dfkjkdblahblahlfnonvoev"
export SLACK_NOTIFICATION_LIST="bobp;Mickey;Minnie123"
```
Also, to send Slack notifications this program uses another small library that I have posted 
here on GitHub: `git clone https://github.com/rcprcp/SimpleSlack`. 

The Slack display names are picked up from the SLACK_NOTIFICATION_LIST, separated by ";". The names must match the Slack Display 
names exactly (e.g. they are case sensitive), only leading and trailing spaces are removed.
If you don't want the Slack notifications, don't set the  SLACK_NOTIFICATION_LIST.  Also, if you don't want to build with 
the SimpleSlack library, feel free to remove it from the pom.xml, and update the code as needed.

## Download the source
```shell
git clone https://github.com/rcprcp/KnowledgeBasePermissions.git
```

## Build 
Use the typical invocations for building a Maven-based application.
```shell
mvn clean package
```

Find the runnable jar in the `target` subdirectory: 
```shell
ls -ld ./target/KnowledgeBasePermissions-1.0-SNAPSHOT-jar-with-dependencies.jar
```
groovy-twitter-followers
==============
Draft project to schedule Groovy script and manage manage followers, favorites, messages, categories

Take care about Twitter ratio API v1.1:

French: https://support.twitter.com/articles/95609-regles-et-bonnes-pratiques-d-abonnement#

API ratio: <br/>
https://dev.twitter.com/rest/public/rate-limiting
https://dev.twitter.com/rest/public/rate-limits

“rate limit exceeded” <br/>
https://blog.twitter.com/2008/what-does-rate-limit-exceeded-mean-updated

The project use Twitter4J:<br/>
GitHub: https://github.com/yusuke/twitter4j/

Website: http://twitter4j.org/


Target of the Groovy Script:
- Manage follow/unfollow
- Manage favorite/unfavorite
- Manage categories
- Clean DM
- Tweet

# Quick Start

## API tokens
Create your own API tokens with read/write access<br/>
https://apps.twitter.com/ "Keys and Access Tokens"<br/>
Consumer Key (API Key)<br/>
Consumer Secret (API Secret)<br/>
Access Token<br/>
Access Token Secret<br/>
Access Level Read and write<br/>

Save the values in your app folder: /conf/MY_TWITTER_ACCOUNT/config.properties

Create credentials for an email service like MailJet: https://www.mailjet.com<br/>
Set the credentials in the same configuration file: /conf/MY_TWITTER_ACCOUNT/config.properties

## Setup the account

Set in datas/YOUR_ACCOUNT/draft_prospects.properties the Twitter accounts you want use to define follow/unfollow list<br/>

The script "setup" will create 
- the "follow protected list" (peoples you want to keep in your friends list)
<pre>datas/YOUR_ACCOUNT/protected_friends.properties</pre>
- the prospect source list : peoples who follow some accounts (from draft_prospects.properties)
<pre>datas/YOUR_ACCOUNT/friends_prospects.properties</pre>

<pre>groovy -classpath classpath -DrootScriptDir="YOUR_PROJECT_PATH" -Dcontext=MY_TWITTER_ACCOUNT twitter4j-setup.groovy</pre>

## Scripts
Run one script : 

Example, the summary script about your account: 

<pre>groovy -classpath classpath -DrootScriptDir="YOUR_PROJECT_PATH" -Dcontext=MY_TWITTER_ACCOUNT twitter4j-summary-profile-management.groovy</pre>

### Add favorite<br/>
<pre>groovy -classpath classpath -DrootScriptDir="YOUR_PROJECT_PATH" -Dcontext=MY_TWITTER_ACCOUNT -Dmode=add.favorite twitter4j-favorite-tweet-management.groovy</pre>

### Clean Favorite<br/>
<pre>groovy -classpath classpath -DrootScriptDir="YOUR_PROJECT_PATH" -Dcontext=MY_TWITTER_ACCOUNT -Dmode=clean.favorite twitter4j-favorite-tweet-management.groovy</pre>

### Add Followers<br/>
<pre>groovy -classpath classpath -DrootScriptDir="YOUR_PROJECT_PATH" -Dcontext=denizzzz twitter4j-followers-management.groovy</pre>

If proxy add:<br/>
<pre>-Dhttp.proxyHost=PROXY_HOST_IF_EXIST -Dhttp.proxyPort=PROXY_PORTS_IF_EXIST -Dhttp.proxyHost=PROXY_SECURE_HOST_IF_EXIST -Dhttp.proxyPort=PROXY_SECURE_PORTS_IF_EXIST</pre>


## TODO

- write : clean DM message (useless messages with "thanks for the follow" or "TrueTwit validation", etc)
- write : manage categories (create category from properties configuration, and follow/unfollow account who tweet some associated key words/hashtags)

- clean common code from twitter4j-*.groovy script
- improve code about twitter rate from classpath scripts


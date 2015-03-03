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

Run <pre>twitter4j-setup.groovy</pre>

## Scripts
Run one script : 

Example: <pre>twitter4j-favorite-tweet-management.groovy</pre>
Add favorite<br/>
<pre>groovy -classpath classpath -DrootScriptDir="YOUR_PROJECT_PATH" -Dcontext=MY_TWITTER_ACCOUNT -Dmode=add.favorite twitter4j-favorite-tweet-management.groovy</pre>

Clean Favorite<br/>
<pre>groovy -classpath classpath -DrootScriptDir="YOUR_PROJECT_PATH" -Dcontext=MY_TWITTER_ACCOUNT -Dmode=clean.favorite twitter4j-favorite-tweet-management.groovy</pre>

Add Followers<br/>
<pre>groovy -classpath classpath -DrootScriptDir="/home/twitter/script/workspace/test/" -Dcontext=denizzzz twitter4j-followers-management.groovy</pre>

If proxy add:<br/>
<pre>-Dhttp.proxyHost=PROXY_HOST_IF_EXIST -Dhttp.proxyPort=PROXY_PORTS_IF_EXIST -Dhttp.proxyHost=PROXY_SECURE_HOST_IF_EXIST -Dhttp.proxyPort=PROXY_SECURE_PORTS_IF_EXIST</pre>

# Manage follow/unfollow


# Manage favorite/unfavorite


# Tweet



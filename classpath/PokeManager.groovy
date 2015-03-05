
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

import java.math.*
 
/**
 *
 */
class PokeManager {

	Twitter twitter
	long[] myFollowers
	def rootScriptDir = System.getProperty("rootScriptDir")
	def userName
	def today = Calendar.instance

    PokeManager(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.userName = appDatasXml.conf.user.@screenName.text()
		def userFolder = new File(rootScriptDir + 'datas/' + userName + '/')
		if(!userFolder.exists()){
			userFolder.mkdirs()  
		}
    }

	def startAddPokeTweets(maxNewPokes){
		IDs ids = twitter.getFollowersIDs(-1);
		this.myFollowers = ids.getIDs();
		
		final Map queryWordsMap = getQueryWords()
		final Map pokeTweetsMap = new HashMap()
		def countNewPokeTweetGlobal = 0
        try {
		
Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
RateLimitStatus rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status")
System.out.println("RateLimit Limit: " + rateLimitStatusApplicationRateLimit.getLimit());
System.out.println("RateLimit Remaining: " + rateLimitStatusApplicationRateLimit.getRemaining());
System.out.println("RateLimit SecondsUntilReset: " + rateLimitStatusApplicationRateLimit.getSecondsUntilReset());
if(rateLimitStatusApplicationRateLimit.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitStatusApplicationRateLimit.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status");
}

RateLimitStatus rateLimitSearchTweet = rateLimitStatusMap.get("/search/tweets")
System.out.println("SearchTweet Limit: " + rateLimitSearchTweet.getLimit());
System.out.println("SearchTweet Remaining: " + rateLimitSearchTweet.getRemaining());
System.out.println("SearchTweet SecondsUntilReset: " + rateLimitSearchTweet.getSecondsUntilReset());
if(rateLimitSearchTweet.getRemaining() < 3){
	ScriptGroovyUtil.pause(rateLimitSearchTweet.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitSearchTweet = rateLimitStatusMap.get("/search/tweets");
}
				
			def splitByWord = getPokeTweetByQueryWord(queryWordsMap, maxNewPokes)
			
			def countWhile = 0
			while(countWhile < maxNewPokes){
				countWhile++
				if(pokeTweetsMap.size() == maxNewPokes){
					continue;
				}
				queryWordsMap.each { queryWord, enabled ->
					println "Start search poke with '" + queryWord + "', enabled: " + enabled 	
					if(enabled){
						Query query = new Query(queryWord);
						query.setCount(150)
						QueryResult result = twitter.search(query);
						List<Status> tweets = result.getTweets();
						println "Query result for '" + queryWord + "', size: " + tweets.size() + ", splitByWord:" + splitByWord + ", maxNewPokes:" + maxNewPokes + ", pokeTweetsMap: " + pokeTweetsMap.size()
						if(tweets.size() == 0){
							queryWordsMap.put(queryWord, false)
							splitByWord = getPokeTweetByQueryWord(queryWordsMap, maxNewPokes)
							println "Disable for this run, Query word '$queryWord'"
						}
						def countNewPokeTweetByWord = 0
						def validTimeStamp = today.time.time - (45 * 60 * 1000)
						boolean nothingToAdd = true
						for (Status tweet : tweets) {
							if(countNewPokeTweetByWord >= splitByWord){
								break
							}
							if(countNewPokeTweetGlobal >= maxNewPokes){
								break
							}
							String tweetUserScreenName = "" + tweet.getUser().getScreenName()
							Date createdAt = tweet.getCreatedAt()
							println "Check Tweet, user id: " + tweetUserScreenName + ", date: " + createdAt.time + ", must be > " + validTimeStamp							
							if(!pokeTweetsMap.containsKey(tweetUserScreenName) && createdAt.time > validTimeStamp){
								def tweetUserId = tweet.getUser().getId()
								boolean isNotFollowingMe = ScriptGroovyUtil.isNotFollowingMe(this.myFollowers, tweetUserId)
								Relationship relationship = twitter.showFriendship(twitter.getId(), tweetUserId)
								println "isSourceFollowedByTarget: " + relationship.isSourceFollowedByTarget() + ", isNotFollowingMe: " + isNotFollowingMe
								if(!relationship.isSourceFollowedByTarget() && isNotFollowingMe){
									println "Add poke, id: " + tweetUserScreenName + ", date: " + createdAt
									pokeTweetsMap.put(tweetUserScreenName, tweet.getUser().getId())
									countNewPokeTweetGlobal++
									countNewPokeTweetByWord++
									nothingToAdd = false
								}
							}
						}
						if(nothingToAdd){
							queryWordsMap.put(queryWord, false)
							splitByWord = getPokeTweetByQueryWord(queryWordsMap, maxNewPokes)
							println "Disable for this run, Query word '$queryWord'"
						}
					}
				}
			}

		} catch (TwitterException ex) {
			println "TwitterException : " + ex.getMessage()
			
			Writer wr = new StringWriter();
			PrintWriter pWriter = new PrintWriter(wr);
			ex.printStackTrace(pWriter);
			SendEmail.sendErrorMail(wr.toString());
				
			if(ex.getRateLimitStatus() != null){
				ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
			}
        }
		
		def countPokeTweet = 0
		pokeTweetsMap.each { key, value ->
			try {
				def pokeStatus = "@$key follow us #poke"
				Status status = twitter.updateStatus(pokeStatus)
				println "Create Poke to: @" + key
				countPokeTweet++
			} catch (TwitterException ex) {
				println "TwitterException : '" + value + "' : " + ex.getMessage()
								
				//Writer wr = new StringWriter();
				//PrintWriter pWriter = new PrintWriter(wr);
				//ex.printStackTrace(pWriter);
				//SendEmail.sendErrorMail(wr.toString());
					
				if(ex.getRateLimitStatus() != null){
					ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset())
				}
			}
		}
	
		println "Poke tweets added: " + countPokeTweet + " on " + pokeTweetsMap.size()
	
		SendEmail.sendSuccessMail("add poke", countNewPokeTweetGlobal);
	}

	def getPokeTweetByQueryWord(queryWordsMap, maxNewPokes){
		def countQueryWordEnabled = 0
		queryWordsMap.each { word, enabled ->
			if(enabled)
				countQueryWordEnabled++
		}
		def splitByWordResult = (maxNewPokes / countQueryWordEnabled)
		def splitByWord = splitByWordResult.setScale(0, BigDecimal.ROUND_UP)
		return splitByWord;
	}
	
	def cleanPokeTweets(){
	
		Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
		RateLimitStatus rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status")
		System.out.println("RateLimit Limit: " + rateLimitStatusApplicationRateLimit.getLimit());
		System.out.println("RateLimit Remaining: " + rateLimitStatusApplicationRateLimit.getRemaining());
		System.out.println("RateLimit SecondsUntilReset: " + rateLimitStatusApplicationRateLimit.getSecondsUntilReset());
		if(rateLimitStatusApplicationRateLimit.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitStatusApplicationRateLimit.getSecondsUntilReset())
			rateLimitStatusMap = twitter.getRateLimitStatus()
			rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status");
		}

		RateLimitStatus rateLimitSearchTweet = rateLimitStatusMap.get("/search/tweets")
		System.out.println("SearchTweet Limit: " + rateLimitSearchTweet.getLimit());
		System.out.println("SearchTweet Remaining: " + rateLimitSearchTweet.getRemaining());
		System.out.println("SearchTweet SecondsUntilReset: " + rateLimitSearchTweet.getSecondsUntilReset());
		if(rateLimitSearchTweet.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitSearchTweet.getSecondsUntilReset())
			rateLimitStatusMap = twitter.getRateLimitStatus()
			rateLimitSearchTweet = rateLimitStatusMap.get("/search/tweets");
		}
		
		Paging paging = new Paging(1, 100)
		List<Status> statuses = twitter.getUserTimeline(paging);
		println "HomeTimeline: " + statuses.size()
		def twitterId = twitter.getId()
		def countPokeDeleted = 0
		def countPokeLoop = 0
		for (Status status : statuses) {
			countPokeLoop++
			def text = status.getText().toLowerCase();
			if(status.getUser().getId() == twitterId){
				println "My tweet: " + text
				if(text.contains("poke") && !text.contains("keep")){
					println "is poke tweet, destroy it"
					twitter.destroyStatus(status.getId())
					countPokeDeleted++
				}
			}
		}
		println "Poke tweets deleted: " + countPokeDeleted + " on " + countPokeLoop
	}

	boolean containsQueryWord(queryWordsMap, text){
		for ( e in queryWordsMap ) {
			if(text.toLowerCase().contains(e.key.toLowerCase())){
				return true;
			}
		}
		return false;
	}
	
	def getHistoryPokeTweets(){
		final Map historyPokeTweetsMap = new HashMap()
		try {
			def historyPokeTweetsFile =  new File(rootScriptDir + 'datas/' + userName + '/history_poke_tweets.properties');
			if (!historyPokeTweetsFile.exists()) {
				historyPokeTweetsFile.createNewFile()  
				return historyPokeTweetsMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(historyPokeTweetsFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					if(inputLine.contains("=")){
						String[] split = inputLine.split("=");
						def twitterUserId = split[0];
						def twitterUserScreenName = split[1];
						historyPokeTweetsMap.put(twitterUserId, twitterUserScreenName);
					}
			} catch(IOException ex) { 
				System.err.println("An IOException was caught!"); 
				ex.printStackTrace(); 
			} finally { 
				try { 
					rd.close(); 
				} catch (IOException ex) { 
					System.err.println("An IOException was caught!"); 
					ex.printStackTrace(); 
				} 
			} 
		} catch (e) {
			throw e
		}
		return historyPokeTweetsMap;
	}
	
	def getQueryWords(){
		final Map queryWordsMap = new HashMap()
		try {
			def queryWordsFile =  new File(rootScriptDir + 'datas/' + userName + '/favorite_tweets_query_words.properties');
			if (!queryWordsFile.exists()) {
				queryWordsFile.createNewFile()  
				return queryWordsMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(queryWordsFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null){
					def lineIsNotEmpty = inputLine?.trim()
					println "inputLine: '" + inputLine + "', empty: " + !lineIsNotEmpty
					if(lineIsNotEmpty){
						queryWordsMap.put(inputLine, true);
					}
				}
			} catch(IOException ex) { 
				System.err.println("An IOException was caught!"); 
				ex.printStackTrace(); 
			} finally { 
				try { 
					rd.close(); 
				} catch (IOException ex) { 
					System.err.println("An IOException was caught!"); 
					ex.printStackTrace(); 
				} 
			} 
		} catch (e) {
			throw e
		}
		return queryWordsMap;
	}
	
}
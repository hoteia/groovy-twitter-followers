
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

import java.math.*
 
/**
 *
 */
class FavoriteManager {

	Twitter twitter
	def rootScriptDir = System.getProperty("rootScriptDir")
	def userName
	def today = Calendar.instance

    FavoriteManager(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.userName = appDatasXml.conf.user.@screenName.text()
		def userFolder = new File(rootScriptDir + 'datas/' + userName + '/')
		if(!userFolder.exists()){
			userFolder.mkdirs()  
		}
    }

	def startAddFavoriteTweets(maxNewFavorites){
		final Map queryWordsMap = getQueryWords()
		final Map favoriteTweetsMap = new HashMap()
		def countNewFavoriteTweetGlobal = 0
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
				
			def splitByWord = getFavoriteTweetByQueryWord(queryWordsMap, maxNewFavorites)
			
			def countWhile = 0
			while(countWhile < maxNewFavorites){
				println "----------------------------------------------------------------"
				countWhile++
				if(favoriteTweetsMap.size() == maxNewFavorites){
					continue;
				}
				queryWordsMap.each { queryWord, enabled ->
					println "------------------------------------------------"
					println "Start search favorite with '" + queryWord + "', enabled: " + enabled 	
					if(enabled){
						Query query = new Query(queryWord);
						query.setCount(150)
						QueryResult result = twitter.search(query);
						List<Status> tweets = result.getTweets();
						println "Query result for '" + queryWord + "', size: " + tweets.size() + ", splitByWord:" + splitByWord + ", maxNewFavorites:" + maxNewFavorites + ", favoriteTweetsMap: " + favoriteTweetsMap.size()
						if(tweets.size() == 0){
							queryWordsMap.put(queryWord, false)
							splitByWord = getFavoriteTweetByQueryWord(queryWordsMap, maxNewFavorites)
							println "Disable for this run, Query word '$queryWord'"
						}
						def countNewFavoriteTweetByWord = 0
						def validTimeStamp = today.time.time - (45 * 60 * 1000)
						boolean nothingToAdd = true
						for (Status tweet : tweets) {
							println "--------------------------------"
							if(countNewFavoriteTweetByWord >= splitByWord){
								break
							}
							if(countNewFavoriteTweetGlobal >= maxNewFavorites){
								break
							}
							String tweetIdString = "" + tweet.getId()
							Date createdAt = tweet.getCreatedAt()
							println "Check Tweet, id: " + tweetIdString + ", date: " + createdAt.time + ", must be > " + validTimeStamp							
							if(!favoriteTweetsMap.containsKey(tweetIdString) && createdAt.time > validTimeStamp){
								println "Add favorite, id: " + tweetIdString + ", date: " + createdAt
								favoriteTweetsMap.put(tweetIdString, tweet.getId())
								countNewFavoriteTweetGlobal++
								countNewFavoriteTweetByWord++
								nothingToAdd = false
							}
						}
						if(nothingToAdd){
							queryWordsMap.put(queryWord, false)
							splitByWord = getFavoriteTweetByQueryWord(queryWordsMap, maxNewFavorites)
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
		favoriteTweetsMap.each { key, value ->
			try {
				twitter.createFavorite(value)
				println "Create Favorite : " + value
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
	
		SendEmail.sendSuccessMail("add favorite", countNewFavoriteTweetGlobal);
	}

	def getFavoriteTweetByQueryWord(queryWordsMap, maxNewFavorites){
		def countQueryWordEnabled = 0
		queryWordsMap.each { word, enabled ->
			if(enabled)
				countQueryWordEnabled++
		}
		def splitByWordResult = (maxNewFavorites / countQueryWordEnabled)
		def splitByWord = splitByWordResult.setScale(0, BigDecimal.ROUND_UP)
		return splitByWord;
	}
	
	def cleanFavoriteTweets(){
	
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
		
		final Map queryWordsMap = getQueryWords()
		int page = 10
		int totalTweets = page * 20
		Paging paging = new Paging(1, totalTweets)
		ResponseList<Status> list = twitter.getFavorites(paging);
		def countFavoriteDeleted = 0
		def countFavoriteLoop = 0
		for (Iterator<Status> iterator = list.iterator(); iterator.hasNext();) {
			Status status = (Status) iterator.next();
			Date createdAt = status.getCreatedAt()
			def text = status.getText().toLowerCase()
			def validTimeStamp = today.time.time - (60 * 60 * 1000)
			countFavoriteLoop++
			println "createdAt: " + createdAt.time + ", validTimeStamp: " + validTimeStamp + ", contains QueryWords: " + containsQueryWord(queryWordsMap, text)
			if(createdAt.time <= validTimeStamp && containsQueryWord(queryWordsMap, text)){
				try {
					twitter.destroyFavorite(status.getId())
					countFavoriteDeleted++
					println "Delete Favorite tweet: '" + status.getId() + "', date: " + createdAt
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
			}
		}
		println "Favorite tweets deleted: " + countFavoriteDeleted + " on " + countFavoriteLoop
	}

	boolean containsQueryWord(queryWordsMap, text){
		for ( e in queryWordsMap ) {
			if(text.toLowerCase().contains(e.key.toLowerCase())){
				return true;
			}
		}
		return false;
	}
	
	def getHistoryFavoriteTweets(){
		final Map historyFavoriteTweetsMap = new HashMap()
		try {
			def historyFavoriteTweetsFile =  new File(rootScriptDir + 'datas/' + userName + '/history_favorite_tweets.properties');
			if (!historyFavoriteTweetsFile.exists()) {
				historyFavoriteTweetsFile.createNewFile()  
				return historyFavoriteTweetsMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(historyFavoriteTweetsFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					if(inputLine.contains("=")){
						String[] split = inputLine.split("=");
						def twitterUserId = split[0];
						def twitterUserScreenName = split[1];
						historyFavoriteTweetsMap.put(twitterUserId, twitterUserScreenName);
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
		return historyFavoriteTweetsMap;
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
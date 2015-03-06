
import twitter4j.*
import twitter4j.auth.*
import twitter4j.conf.*

/**
 *
 */
class RateUtil {

	synchronized static RateLimitStatus checkRateLimit(Twitter twitter){
		Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
		def apiPath = "/application/rate_limit_status"
		
		RateLimitStatus rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get(apiPath)
		System.out.println("RateLimit Limit: " + rateLimitStatusApplicationRateLimit.getLimit());
		System.out.println("RateLimit Remaining: " + rateLimitStatusApplicationRateLimit.getRemaining());
		System.out.println("RateLimit SecondsUntilReset: " + rateLimitStatusApplicationRateLimit.getSecondsUntilReset());
		if(rateLimitStatusApplicationRateLimit.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitStatusApplicationRateLimit.getSecondsUntilReset())
			rateLimitStatusMap = twitter.getRateLimitStatus()
			rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get(apiPath);
		}
		return rateLimitStatusApplicationRateLimit
	}
	
	synchronized static RateLimitStatus checkRateLimitSearchTweet(Twitter twitter){
		Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
		def apiPath = "/search/tweets"
		RateUtil.checkRateLimit(twitter)
		
		RateLimitStatus rateLimitSearchTweet = rateLimitStatusMap.get(apiPath)
		System.out.println("UsersShow Limit: " + rateLimitSearchTweet.getLimit());
		System.out.println("UsersShow Remaining: " + rateLimitSearchTweet.getRemaining());
		//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitSearchTweet.getResetTimeInSeconds());
		System.out.println("UsersShow SecondsUntilReset: " + rateLimitSearchTweet.getSecondsUntilReset());
		if(rateLimitSearchTweet.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitSearchTweet.getSecondsUntilReset())
			rateLimitStatusMap = twitter.getRateLimitStatus()
			rateLimitSearchTweet = rateLimitStatusMap.get(apiPath)
		}
		return rateLimitSearchTweet
	}
	
	synchronized static RateLimitStatus checkRateLimitStatusFollowers(Twitter twitter){
		Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
		def apiPath = "/followers/ids"
		RateUtil.checkRateLimit(twitter)
		
		RateLimitStatus rateLimitStatusFollowers = rateLimitStatusMap.get(apiPath)
		System.out.println("UsersShow Limit: " + rateLimitStatusFollowers.getLimit());
		System.out.println("UsersShow Remaining: " + rateLimitStatusFollowers.getRemaining());
		//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitStatusFollowers.getResetTimeInSeconds());
		System.out.println("UsersShow SecondsUntilReset: " + rateLimitStatusFollowers.getSecondsUntilReset());
		if(rateLimitStatusFollowers.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitStatusFollowers.getSecondsUntilReset())
			rateLimitStatusMap = twitter.getRateLimitStatus()
			rateLimitStatusFollowers = rateLimitStatusMap.get(apiPath)
		}
		return rateLimitStatusFollowers
	}
	
	synchronized static RateLimitStatus checkRateLimitUserShow(Twitter twitter){
		Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
		def apiPath = "/users/show/:id"
		RateUtil.checkRateLimit(twitter)
		
		RateLimitStatus rateLimitUserShow = rateLimitStatusMap.get(apiPath)
		System.out.println("UsersShow Limit: " + rateLimitUserShow.getLimit());
		System.out.println("UsersShow Remaining: " + rateLimitUserShow.getRemaining());
		//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitUserShow.getResetTimeInSeconds());
		System.out.println("UsersShow SecondsUntilReset: " + rateLimitUserShow.getSecondsUntilReset());
		if(rateLimitUserShow.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
			rateLimitStatusMap = twitter.getRateLimitStatus()
			rateLimitUserShow = rateLimitStatusMap.get(apiPath)
		}
		return rateLimitUserShow
	}
	
	synchronized static RateLimitStatus checkRateLimitFriends(Twitter twitter){
		Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
		def apiPath = "/friends/ids"
		RateUtil.checkRateLimit(twitter)
		
		RateLimitStatus rateLimitFriends = rateLimitStatusMap.get(apiPath)
		System.out.println("UsersShow Limit: " + rateLimitFriends.getLimit());
		System.out.println("UsersShow Remaining: " + rateLimitFriends.getRemaining());
		//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitFriends.getResetTimeInSeconds());
		System.out.println("UsersShow SecondsUntilReset: " + rateLimitFriends.getSecondsUntilReset());
		if(rateLimitFriends.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitFriends.getSecondsUntilReset())
			rateLimitStatusMap = twitter.getRateLimitStatus()
			rateLimitFriends = rateLimitStatusMap.get(apiPath)
		}
		return rateLimitFriends
	}
	
}

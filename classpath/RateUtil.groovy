
import twitter4j.*
import twitter4j.auth.*
import twitter4j.conf.*

/**
 *
 */
class RateUtil {

	synchronized static Map<String, RateLimitStatus> checkRateLimit(Map<String, RateLimitStatus> rateLimitStatusMap, Twitter twitter){
		if(rateLimitStatusMap == null){
			rateLimitStatusMap = twitter.getRateLimitStatus()
		}
		def apiPath = "/application/rate_limit_status"
		
		RateLimitStatus rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get(apiPath)
		System.out.println("RateLimit Limit: " + rateLimitStatusApplicationRateLimit.getLimit());
		System.out.println("RateLimit Remaining: " + rateLimitStatusApplicationRateLimit.getRemaining());
		System.out.println("RateLimit SecondsUntilReset: " + rateLimitStatusApplicationRateLimit.getSecondsUntilReset());
		if(rateLimitStatusApplicationRateLimit.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitStatusApplicationRateLimit.getSecondsUntilReset())
			rateLimitStatusMap = twitter.getRateLimitStatus()
		}
		return rateLimitStatusMap
	}
	
	synchronized static RateLimitStatus checkRateLimitSearchTweet(Map<String, RateLimitStatus> rateLimitStatusMap, Twitter twitter){
		if(rateLimitStatusMap == null){
			rateLimitStatusMap = RateUtil.checkRateLimit(rateLimitStatusMap, twitter)
		}
		def apiPath = "/search/tweets"		
		
		RateLimitStatus rateLimitSearchTweet = rateLimitStatusMap.get(apiPath)
		System.out.println("UsersShow Limit: " + rateLimitSearchTweet.getLimit());
		System.out.println("UsersShow Remaining: " + rateLimitSearchTweet.getRemaining());
		//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitSearchTweet.getResetTimeInSeconds());
		System.out.println("UsersShow SecondsUntilReset: " + rateLimitSearchTweet.getSecondsUntilReset());
		if(rateLimitSearchTweet.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitSearchTweet.getSecondsUntilReset())
			rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
			rateLimitSearchTweet = rateLimitStatusMap.get(apiPath)
		}
		return rateLimitSearchTweet
	}
	
	synchronized static RateLimitStatus checkRateLimitStatusFollowers(Map<String, RateLimitStatus> rateLimitStatusMap, Twitter twitter){
		if(rateLimitStatusMap == null){
			rateLimitStatusMap = RateUtil.checkRateLimit(rateLimitStatusMap, twitter)
		}
		def apiPath = "/followers/ids"
		
		RateLimitStatus rateLimitStatusFollowers = rateLimitStatusMap.get(apiPath)
		System.out.println("UsersShow Limit: " + rateLimitStatusFollowers.getLimit());
		System.out.println("UsersShow Remaining: " + rateLimitStatusFollowers.getRemaining());
		//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitStatusFollowers.getResetTimeInSeconds());
		System.out.println("UsersShow SecondsUntilReset: " + rateLimitStatusFollowers.getSecondsUntilReset());
		if(rateLimitStatusFollowers.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitStatusFollowers.getSecondsUntilReset())
			rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
			rateLimitStatusFollowers = rateLimitStatusMap.get(apiPath)
		}
		return rateLimitStatusFollowers
	}
	
	synchronized static RateLimitStatus checkRateLimitUserShow(Map<String, RateLimitStatus> rateLimitStatusMap, Twitter twitter){
		if(rateLimitStatusMap == null){
			rateLimitStatusMap = RateUtil.checkRateLimit(rateLimitStatusMap, twitter)
		}
		def apiPath = "/users/show/:id"
		
		RateLimitStatus rateLimitUserShow = rateLimitStatusMap.get(apiPath)
		System.out.println("UsersShow Limit: " + rateLimitUserShow.getLimit());
		System.out.println("UsersShow Remaining: " + rateLimitUserShow.getRemaining());
		//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitUserShow.getResetTimeInSeconds());
		System.out.println("UsersShow SecondsUntilReset: " + rateLimitUserShow.getSecondsUntilReset());
		if(rateLimitUserShow.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
			rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
			rateLimitUserShow = rateLimitStatusMap.get(apiPath)
		}
		return rateLimitUserShow
	}
	
	synchronized static RateLimitStatus checkRateLimitFriends(Map<String, RateLimitStatus> rateLimitStatusMap, Twitter twitter){
		if(rateLimitStatusMap == null){
			rateLimitStatusMap = RateUtil.checkRateLimit(rateLimitStatusMap, twitter)
		}
		def apiPath = "/friends/ids"
		
		RateLimitStatus rateLimitFriends = rateLimitStatusMap.get(apiPath)
		System.out.println("UsersShow Limit: " + rateLimitFriends.getLimit());
		System.out.println("UsersShow Remaining: " + rateLimitFriends.getRemaining());
		//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitFriends.getResetTimeInSeconds());
		System.out.println("UsersShow SecondsUntilReset: " + rateLimitFriends.getSecondsUntilReset());
		if(rateLimitFriends.getRemaining() < 3){
			ScriptGroovyUtil.pause(rateLimitFriends.getSecondsUntilReset())
			rateLimitStatusMap = RateUtil.checkRateLimit(null, twitter)
			rateLimitFriends = rateLimitStatusMap.get(apiPath)
		}
		return rateLimitFriends
	}
	
}
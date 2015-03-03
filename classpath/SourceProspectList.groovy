
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

/**
 *
 */
class SourceProspectList {

	Twitter twitter
	def rootScriptDir = System.getProperty("rootScriptDir")
	def userName
	
    SourceProspectList(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.userName = appDatasXml.conf.user.@screenName.text()
		def userFolder = new File(rootScriptDir + 'datas/' + userName + '/')
		if(!userFolder.exists()){
			userFolder.mkdirs()  
		}
    }

	def isNotUpToDate(){
		final Map sourceProspectsMap = getSourceProspects();
		if(sourceProspectsMap.size() > 0){
			final Map draftProspectsMap = getDraftProspects();
			if(draftProspectsMap.size() > 0){
				println "SOURCE PROSPECT LIST IS INITIALIZED - BUT SOME NEW TARGETS NEED TO BE EVALUATE"
				return true;
			}
			return false;
		} else {
			println "SOURCE PROSPECT LIST IS NOT INITIALIZED"
			return true;
		}
	}
	
	def initialize(){
		final Map newSourceProspectsMap = new HashMap()
		final Map draftProspectsMap = getDraftProspects()
		if(draftProspectsMap.size() > 0){
			// CHECK RATIO
			Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
			RateLimitStatus rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status");
			System.out.println("RateLimit Limit: " + rateLimitStatusApplicationRateLimit.getLimit());
			System.out.println("RateLimit Remaining: " + rateLimitStatusApplicationRateLimit.getRemaining());
			//System.out.println("RateLimit ResetTimeInSeconds: " + rateLimitStatusApplicationRateLimit.getResetTimeInSeconds());
			System.out.println("RateLimit SecondsUntilReset: " + rateLimitStatusApplicationRateLimit.getSecondsUntilReset());
			if(rateLimitStatusApplicationRateLimit.getRemaining() < 3){
				ScriptGroovyUtil.pause(rateLimitStatusApplicationRateLimit.getSecondsUntilReset())
				rateLimitStatusMap = twitter.getRateLimitStatus()
				rateLimitStatusApplicationRateLimit = rateLimitStatusMap.get("/application/rate_limit_status");
			}

			RateLimitStatus rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id");
			System.out.println("UsersShow Limit: " + rateLimitStatusUsers.getLimit());
			System.out.println("UsersShow Remaining: " + rateLimitStatusUsers.getRemaining());
			//System.out.println("UsersShow ResetTimeInSeconds: " + rateLimitStatusUsers.getResetTimeInSeconds());
			System.out.println("UsersShow SecondsUntilReset: " + rateLimitStatusUsers.getSecondsUntilReset());
			if(rateLimitStatusUsers.getRemaining()< 3){
				ScriptGroovyUtil.pause(rateLimitStatusUsers.getSecondsUntilReset())
				rateLimitStatusMap = twitter.getRateLimitStatus()
				rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id");
			}
			
			def countCallTwitterShowUser = 0
			draftProspectsMap.each { key, value ->
				try{
// CHECK RATIO
println "CHECK RATIO : " + rateLimitStatusUsers.getRemaining() + " : " + countCallTwitterShowUser 
if(countCallTwitterShowUser >= (rateLimitStatusUsers.getRemaining() - 1)){
	ScriptGroovyUtil.pause(rateLimitStatusUsers.getSecondsUntilReset())
	rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitStatusUsers = rateLimitStatusMap.get("/users/show/:id")
	countCallTwitterShowUser = 0
}

					User user = twitter.showUser(key)
					countCallTwitterShowUser++
					def twitterUserInfo = user.getScreenName() + ";" + user.getName()
					newSourceProspectsMap.put(user.getId(), twitterUserInfo)
				} catch(TwitterException ex) { 
					println "TwitterException : " + ex.getMessage()
					ScriptGroovyUtil.pause(ex.getRateLimitStatus().getSecondsUntilReset());
				} catch(IOException ex) { 
					System.err.println("An IOException was caught!");
					ex.printStackTrace();
				}
			}
			
			// SAVE/ADD NEW PROSPECT
			// rewrite all the map : delete/write
			File file = new File(rootScriptDir + 'datas/' + userName + '/friends_prospects.properties');
			println "prospect to write in the prospect source list: " + newSourceProspectsMap.entrySet().size()
			newSourceProspectsMap.each { key, value ->
				try{
					 file << (key + "=" + value + "\n")
				} catch(IOException ex) { 
					System.err.println("An IOException was caught!"); 
					ex.printStackTrace(); 
				}
			}

			// clean draft			
			def draftProspectsFile =  new File(rootScriptDir + 'datas/' + userName + '/draft_prospects.properties');
			draftProspectsFile.delete()
			draftProspectsFile.createNewFile()  
			
		} else {
			println "DRAFT SOURCE PROSPECT LIST IS EMPTY - WE CAN'T CONTINUE"
		}		
	}

	def getSourceProspects(){
		final Map sourceProspectsMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(rootScriptDir + 'datas/' + userName + '/friends_prospects.properties');
			if (!protectedFollowingFile.exists()) {
				protectedFollowingFile.createNewFile()  
				return sourceProspectsMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(protectedFollowingFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					if(inputLine.contains("=")){
						String[] split = inputLine.split("=");
						def twitterUserId = split[0];
						def twitterUserScreenName = split[1].split(";");
						sourceProspectsMap.put(twitterUserId, split[1]);
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
		return sourceProspectsMap;
	}
	
	def getDraftProspects(){
		final Map draftProspectsMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(rootScriptDir + 'datas/' + userName + '/draft_prospects.properties');
			if (!protectedFollowingFile.exists()) {
				protectedFollowingFile.createNewFile()  
				return draftProspectsMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(protectedFollowingFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					draftProspectsMap.put(inputLine.trim(), inputLine.trim());
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
		return draftProspectsMap;
	}
		
}
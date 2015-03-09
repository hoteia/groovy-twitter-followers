
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

/**
 *
 */
class SourceProspectList {

	Twitter twitter
	def appDatasXml

	def userName
	
    SourceProspectList(Twitter twitter, def appDatasXml) {
		this.twitter = twitter;	
		this.appDatasXml = appDatasXml;	
		
		this.userName = appDatasXml.conf.user.@screenName.text()
    }

	def isNotUpToDate(){
		final Map sourceProspectsMap = MapUtil.getSourceProspects(appDatasXml);
		if(sourceProspectsMap.size() > 0){
			final Map draftProspectsMap = MapUtil.getDraftProspects(appDatasXml);
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
		final Map draftProspectsMap = MapUtil.getDraftProspects(appDatasXml)
		if(draftProspectsMap.size() > 0){
		
			RateLimitStatus rateLimitUserShow = RateUtil.checkRateLimitUserShow(twitter)
			
			def countCallTwitterShowUser = 0
			draftProspectsMap.each { key, value ->
				try{
// CHECK RATIO
println "CHECK RATIO : " + rateLimitUserShow.getRemaining() + " : " + countCallTwitterShowUser 
if(countCallTwitterShowUser >= (rateLimitUserShow.getRemaining() - 1)){
	ScriptGroovyUtil.pause(rateLimitUserShow.getSecondsUntilReset())
	Map<String ,RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus()
	rateLimitUserShow = rateLimitStatusMap.get("/users/show/:id")
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
			File file = new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/friends_prospects.properties');
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
			def draftProspectsFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/draft_prospects.properties');
			draftProspectsFile.delete()
			draftProspectsFile.createNewFile()  
			
		} else {
			println "DRAFT SOURCE PROSPECT LIST IS EMPTY - WE CAN'T CONTINUE"
		}		
	}
		
}
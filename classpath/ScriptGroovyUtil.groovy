
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

/**
 *
 */
class ScriptGroovyUtil {

	synchronized static void pause(Long time) {
		def today = new Date()
		/*
		if(time > 0){
			def timeBeforeContinue = (time + 30) * 1000
			println "Wait API Quota Twitter, now: $today, pause: $time, we will wait: " + timeBeforeContinue + " (millisecond)"
			wait(timeBeforeContinue)
		} else {
			println "Wait API Quota Twitter, now: $today, pause: $time, we will wait time>15min: 1000000 (millisecond)"
			wait(1000000)
		}
		*/
		println "Wait API Quota Twitter, now: $today, pause: $time, we will wait time>15min: 1000000 (millisecond)"
		wait(1000000)
    }

	static boolean isNotFollowingMe(long[] myFollowers, long targetFollowerId){
		for (int i = 0; i < myFollowers.length; i++) {
			long followerId = myFollowers[i];
			if(followerId == targetFollowerId){
				return false;
			}
		}
		return true
	}
	
	static String getRootScriptDir(){
		def rootScriptDir = System.getProperty("rootScriptDir")
		return rootScriptDir
	}

	static String getDataPath(userName){
		def dataPath = ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName.toLowerCase()
		return dataPath
	}
	
}

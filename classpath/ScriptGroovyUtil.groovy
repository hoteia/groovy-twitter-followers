
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

/**
 *
 */
class ScriptGroovyUtil {

	synchronized static void pause(Long time) {
		if(time > 0){
			def timeBeforeContinue = (time + 30) * 1000
			println "Wait API Quota Twitter, $time, we will wait: " + timeBeforeContinue + " (millisecond)"
			wait(timeBeforeContinue)
		} else {
			println "Wait API Quota Twitter, $time, we will wait time>15min: 1000000 (millisecond)"
			wait(1000000)
		}
    }

}


import groovy.json.*

/**
 *
 */
class DataManager {

	def static jsonSlurper = new JsonSlurper()

	synchronized static Map getHistoricIds(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map historicMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(ScriptGroovyUtil.getDataPath(userName) + '/history_add_followers.properties');
			if (!protectedFollowingFile.exists()) {
				protectedFollowingFile.createNewFile()  
				return historicMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(protectedFollowingFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					if(inputLine.contains("=")){
						String[] split = inputLine.split("=");
						def twitterUserId = split[0];
						def content = split[1];
						if(content.startsWith("{")){
							def twitterUser = jsonSlurper.parseText(content)
							historicMap.put(twitterUserId, twitterUser);
						} else {
							def twitterUserScreenName = content.split(";");
							historicMap.put(twitterUserId, twitterUserScreenName[0]);
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
		return historicMap;
	}
	
	synchronized static Map getBlockedByUsers(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map blockedByUsersMap = new HashMap()
		try {
			def blockedByUsersFile =  new File(ScriptGroovyUtil.getDataPath(userName) + '/blocked_by_user.properties');
			if (!blockedByUsersFile.exists()) {
				blockedByUsersFile.createNewFile()  
				return blockedByUsersMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(blockedByUsersFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					if(inputLine.contains("=")){
						String[] split = inputLine.split("=");
						def twitterUserId = split[0];
						def content = split[1];
						if(content.startsWith("{")){
							def twitterUser = jsonSlurper.parseText(content)
							blockedByUsersMap.put(twitterUserId, twitterUser);
						} else {
							def twitterUserScreenName = content.split(";");
							blockedByUsersMap.put(twitterUserId, twitterUserScreenName[0]);
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
		return blockedByUsersMap;
	}
	
	synchronized static Map getIgnoreFollowers(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map ignoreFollowersMap = new HashMap()
		try {
			def ignoreFollowersFile =  new File(ScriptGroovyUtil.getDataPath(userName) + '/ignore_followers.properties');
			if (!ignoreFollowersFile.exists()) {
				ignoreFollowersFile.createNewFile()  
				return ignoreFollowersMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(ignoreFollowersFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					if(inputLine.contains("=")){
						String[] split = inputLine.split("=");
						def twitterUserId = split[0];
						def content = split[1];
						if(content.startsWith("{")){
							def twitterUser = jsonSlurper.parseText(content)
							ignoreFollowersMap.put(twitterUserId, twitterUser);
						} else {
							def twitterUserScreenName = content.split(";");
							ignoreFollowersMap.put(twitterUserId, twitterUserScreenName[0]);
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
		return ignoreFollowersMap;
	}
	
	synchronized static Map getHistoryFavoriteTweets(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map historyFavoriteTweetsMap = new HashMap()
		try {
			def historyFavoriteTweetsFile =  new File(ScriptGroovyUtil.getDataPath(userName) + '/history_favorite_tweets.properties');
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
						def content = split[1];
						if(content.startsWith("{")){
							def twitterUser = jsonSlurper.parseText(content)
							historyFavoriteTweetsMap.put(twitterUserId, twitterUser);
						} else {
							def twitterUserScreenName = content.split(";");
							historyFavoriteTweetsMap.put(twitterUserId, twitterUserScreenName[0]);
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
		return historyFavoriteTweetsMap;
	}
	
	synchronized static Map getProtectedFriendsMap(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map protectedFriendsMap = new HashMap()
		try {
			def path = ScriptGroovyUtil.getDataPath(userName) + '/protected_friends.properties'
			def protectedFollowingFile =  new File(path)
			if (!protectedFollowingFile.exists()) {
				println "Create file: $path"
				protectedFollowingFile.createNewFile()  
				return protectedFriendsMap;
			}
			BufferedReader rd = null; 
			try { 
				rd = new BufferedReader(new FileReader(protectedFollowingFile)); 
				String inputLine = null; 
				while((inputLine = rd.readLine()) != null)
					if(inputLine.contains("=")){
						String[] split = inputLine.split("=");
						def twitterUserId = split[0];
						def content = split[1];
						if(content.startsWith("{")){
							def twitterUser = jsonSlurper.parseText(content)
							protectedFriendsMap.put(twitterUserId, twitterUser);
						} else {
							def twitterUserScreenName = content.split(";");
							protectedFriendsMap.put(twitterUserId, twitterUserScreenName[0]);
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
		return protectedFriendsMap;
	}
	
	synchronized static Map getSourceProspects(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map sourceProspectsMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(ScriptGroovyUtil.getDataPath(userName) + '/friends_prospects.properties');
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
						def content = split[1];
						if(content.startsWith("{")){
							def twitterUser = jsonSlurper.parseText(content)
							sourceProspectsMap.put(twitterUserId, twitterUser);
						} else {
							def twitterUserScreenName = content.split(";");
							sourceProspectsMap.put(twitterUserId, twitterUserScreenName[0]);
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
		return sourceProspectsMap;
	}
	
	def getHistoryPokeTweets(){
		final Map historyPokeTweetsMap = new HashMap()
		try {
			def historyPokeTweetsFile =  new File(ScriptGroovyUtil.getDataPath(userName) + '/history_poke_tweets.properties');
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
						def content = split[1];
						if(content.startsWith("{")){
							def twitterUser = jsonSlurper.parseText(content)
							historyPokeTweetsMap.put(twitterUserId, twitterUser);
						} else {
							def twitterUserScreenName = content.split(";");
							historyPokeTweetsMap.put(twitterUserId, twitterUserScreenName[0]);
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
		return historyPokeTweetsMap;
	}
	
	synchronized static Map getDraftProspects(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map draftProspectsMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(ScriptGroovyUtil.getDataPath(userName) + '/draft_prospects.properties');
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
	
	synchronized static Map getQueryWords(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map queryWordsMap = new HashMap()
		try {
			def queryWordsFile =  new File(ScriptGroovyUtil.getDataPath(userName) + '/favorite_tweets_query_words.properties');
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
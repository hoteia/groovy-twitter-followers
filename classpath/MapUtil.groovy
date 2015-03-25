

/**
 *
 */
class MapUtil {

	

	synchronized static Map getHistoricIds(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map historicMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/history_add_followers.properties');
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
						def twitterUserScreenName = split[1].split(";");
						historicMap.put(twitterUserId, twitterUserScreenName);
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
			def blockedByUsersFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/blocked_by_user.properties');
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
						def twitterUserScreenName = split[1].split(";");
						blockedByUsersMap.put(twitterUserId, split[1]);
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
			def ignoreFollowersFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/ignore_followers.properties');
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
						def twitterUserScreenName = split[1].split(";");
						ignoreFollowersMap.put(twitterUserId, split[1]);
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
			def historyFavoriteTweetsFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/history_favorite_tweets.properties');
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
	
	synchronized static Map getQueryWords(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map queryWordsMap = new HashMap()
		try {
			def queryWordsFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/favorite_tweets_query_words.properties');
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
	
	synchronized static Map getProtectedFriendsMap(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map protectedFriendsMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/protected_friends.properties');
			if (!protectedFollowingFile.exists()) {
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
						def twitterUserScreenName = split[1].split(";");
						protectedFriendsMap.put(twitterUserId, split[1]);					
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
			def protectedFollowingFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/friends_prospects.properties');
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
	
	synchronized static Map getDraftProspects(appDatasXml){
		def userName = appDatasXml.conf.user.@name.text()
		
		final Map draftProspectsMap = new HashMap()
		try {
			def protectedFollowingFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/draft_prospects.properties');
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
	
	def getHistoryPokeTweets(){
		final Map historyPokeTweetsMap = new HashMap()
		try {
			def historyPokeTweetsFile =  new File(ScriptGroovyUtil.getRootScriptDir() + 'datas/' + userName + '/history_poke_tweets.properties');
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
	
}
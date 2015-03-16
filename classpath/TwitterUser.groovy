
class TwitterUser {

    long id
    String name
    String screenName
	Date createdAt
    boolean exist = true
    boolean suspended = false
	
    long favouritesCount
    long friendsCount
    long followersCount

    boolean usedAsProspect

    boolean followDryOut
    long followLastCursor
	Date lastFollow
	
}
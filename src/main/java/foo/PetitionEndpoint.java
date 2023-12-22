package foo;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Transaction;
import com.google.api.server.spi.auth.common.User;

@Api(
    name = "myApi",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "tinypet.example.com",
        ownerName = "tinypet.example.com",
        packagePath = "")
)
public class PetitionEndpoint {

    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    @ApiMethod(name = "createPetition", path = "createPetition", httpMethod = ApiMethod.HttpMethod.POST)
    public Entity createPetition(User user, 
                                 @Named("title") String title, 
                                 @Named("description") String description, 
                                 @Named("tags") String tags) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("User must be signed in to create a petition.");
        }
        
        Entity petitionEntity = new Entity("Petition");
        petitionEntity.setProperty("owner", user.getEmail());
        petitionEntity.setProperty("title", title);
        petitionEntity.setProperty("description", description);
        petitionEntity.setProperty("tags", tags.split(","));
        petitionEntity.setProperty("createdDate", new Date());
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(petitionEntity);
        
        return petitionEntity;
    }


    @ApiMethod(name = "signPetition", path = "signPetition/{petitionId}", httpMethod = ApiMethod.HttpMethod.POST)
    public void signPetition(User user, @Named("petitionId") Long petitionId) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("User must be signed in.");
        }

        Key petitionKey = KeyFactory.createKey("Petition", petitionId);
        try {
            Entity petitionEntity = datastore.get(petitionKey);
            ArrayList<String> signatures = (ArrayList<String>) petitionEntity.getProperty("signatures");
            if (signatures == null) {
                signatures = new ArrayList<>();
            }
            String userEmail = user.getEmail();
            if (!signatures.contains(userEmail)) {
                signatures.add(userEmail);
                petitionEntity.setProperty("signatures", signatures);
                datastore.put(petitionEntity);
            } else {
                throw new UnauthorizedException("User has already signed this petition.");
            }
        } catch (EntityNotFoundException e) {
            throw new UnauthorizedException("Petition not found.");
        }
    }

    @ApiMethod(name = "getMySignedPetitions", httpMethod = ApiMethod.HttpMethod.GET)
    public List<Entity> getMySignedPetitions(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("User must be signed in.");
        }
        Query query = new Query("Petition").setFilter(new Query.FilterPredicate("signatures", Query.FilterOperator.EQUAL, user.getEmail()));
        List<Entity> results = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
        return results;
    }

    @ApiMethod(name = "getTopPetitions", httpMethod = ApiMethod.HttpMethod.GET)
    public List<Entity> getTopPetitions() {
        Query query = new Query("Petition").addSort("signatureCount", Query.SortDirection.DESCENDING);
        List<Entity> results = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(100));
        return results;
    }
}
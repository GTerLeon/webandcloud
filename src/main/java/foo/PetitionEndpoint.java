package foo;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.api.server.spi.auth.EspAuthenticator;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Transaction;

@Api(name = "myApi",
     version = "v1",
     namespace = @ApiNamespace(ownerDomain = "helloworld.example.com",
                               ownerName = "helloworld.example.com",
                               packagePath = "")
)

public class PetitionEndpoint {

    // ...

    @ApiMethod(name = "createPetition", path = "createPetition", httpMethod = HttpMethod.POST)
    public Entity createPetition(@Named("title") String title,
                                 @Named("description") String description,
                                 @Named("tags") String tags) {
        Entity petitionEntity = new Entity("Petition");
        petitionEntity.setProperty("title", title);
        petitionEntity.setProperty("description", description);
        petitionEntity.setProperty("tags", tags);
        petitionEntity.setProperty("signatureCount", 0); // Start with zero signatures
        petitionEntity.setProperty("createdDate", new Date()); // Set the current date as the creation date

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(petitionEntity); // Save the entity to the Datastore

        return petitionEntity;
    }

    @ApiMethod(name = "userSignedPetitions", path = "userSignedPetitions/{userId}", httpMethod = HttpMethod.GET)
    public List<Entity> userSignedPetitions(@Named("userId") String userId) {
        // Create a query for petitions signed by the user
        Query q = new Query("Petition").setFilter(new FilterPredicate("signatures", FilterOperator.EQUAL, userId));

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = datastore.prepare(q);
        List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(100));

        return result;
    }

}

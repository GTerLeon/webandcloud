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
     audiences = "927375242383-t21v9ml38tkh2pr30m4hqiflkl3jfohl.apps.googleusercontent.com",
  	 clientIds = {"927375242383-t21v9ml38tkh2pr30m4hqiflkl3jfohl.apps.googleusercontent.com",
        "927375242383-jm45ei76rdsfv7tmjv58tcsjjpvgkdje.apps.googleusercontent.com"},
     namespace =
     @ApiNamespace(
		   ownerDomain = "helloworld.example.com",
		   ownerName = "helloworld.example.com",
		   packagePath = "")
     )
public class PetitionEndpoint {

    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    @ApiMethod(name = "createPetition", httpMethod = HttpMethod.POST)
    public Entity createPetition(@Named("title") String title, @Named("description") String description) throws UnauthorizedException {
        Key key = datastore.allocateId(datastore.newKeyFactory().setKind("Petition").newKey());
        Entity petition = new Entity("Petition", key);
        petition.setProperty("title", title);
        petition.setProperty("description", description);
        petition.setProperty("signatureCount", 0); // Initialize signature count
        petition.setProperty("createdDate", new Date()); // Store the creation date

        datastore.put(petition);
        return petition;
    }

    @ApiMethod(name = "listPetitions", httpMethod = HttpMethod.GET)
    public List<Entity> listPetitions() {
        Query q = new Query("Petition").addSort("createdDate", SortDirection.DESCENDING);

        PreparedQuery pq = datastore.prepare(q);
        List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(100));
        return result;
    }

    // ... (other methods would need to be adapted similarly)

    // This method could be used to sign a petition, ensuring a user cannot sign twice.
    @ApiMethod(name = "signPetition", httpMethod = HttpMethod.POST)
    public void signPetition(@Named("petitionId") long petitionId, User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Retrieve the petition entity by ID
        Key petitionKey = datastore.newKeyFactory().setKind("Petition").newKey(petitionId);
        Transaction txn = datastore.beginTransaction();

        try {
            Entity petition = datastore.get(petitionKey);
            // Increment the signature count
            long signatureCount = (long) petition.getProperty("signatureCount");
            petition.setProperty("signatureCount", signatureCount + 1);
            datastore.put(txn, petition);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    // ... (other methods for managing petitions)

}

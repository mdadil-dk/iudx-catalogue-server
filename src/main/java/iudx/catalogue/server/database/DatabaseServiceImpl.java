package iudx.catalogue.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import java.io.IOException;

/**
 * The Database Service Implementation.
 *
 * <h1>Database Service Implementation</h1>
 *
 * <p>
 * The Database Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.database.DatabaseService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);
  private final RestClient client;

  public DatabaseServiceImpl(RestClient client) {
    this.client = client;
  }

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    /* Initialize elastic clients and JsonObjects */
    Request elasticRequest;
    JsonObject errorJson = new JsonObject();
    request.put(Constants.SEARCH, true);
    // TODO: Stub code, to be removed

    // if (!request.containsKey("instanceId")) {
    // errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION, "No instanceId
    // found");
    // handler.handle(Future.failedFuture(errorJson.toString()));
    // return null;
    // }

    /* Validate the Request */
    if (!request.containsKey(Constants.SEARCH_TYPE)) {
      errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
          Constants.NO_SEARCH_TYPE_FOUND);
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    /* Construct an elastic client request with index to query */
    elasticRequest =
        new Request(Constants.REQUEST_GET, Constants.CAT_TEST_SEARCH_INDEX + Constants.FILTER_PATH);
    /* Construct the query to be made */
    JsonObject query = queryDecoder(request);
    if (query.containsKey(Constants.ERROR)) {
      logger.info("Query returned with an error");
      errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
          query.getString(Constants.ERROR));
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    logger.info("Query constructed: " + query.toString());
    /* Set the elastic client with the query to perform */
    elasticRequest.setJsonEntity(query.toString());
    /* Execute the query */
    client.performRequestAsync(elasticRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        logger.info("Successful DB request");
        JsonArray dbResponse = new JsonArray();
        JsonObject dbResponseJson = new JsonObject();
        try {
          int statusCode = response.getStatusLine().getStatusCode();
          /* Validate the response */
          if (statusCode != 200 && statusCode != 204) {
            handler.handle(Future.failedFuture("Status code is not 2xx"));
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
              .getInteger(Constants.VALUE) == 0) {
            errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                Constants.EMPTY_RESPONSE);
            handler.handle(Future.failedFuture(errorJson.toString()));
            return;
          }
          JsonArray responseHits =
              responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS);
          /* Construct the client response, remove the _source field */
          for (Object json : responseHits) {
            JsonObject jsonTemp = (JsonObject) json;
            dbResponse.add(jsonTemp.getJsonObject(Constants.SOURCE));
          }
          dbResponseJson.put(Constants.STATUS, Constants.SUCCESS)
              .put(Constants.TOTAL_HITS, responseJson.getJsonObject(Constants.HITS)
                  .getJsonObject(Constants.TOTAL).getInteger(Constants.VALUE))
              .put(Constants.RESULT, dbResponse);
          /* Send the response */
          handler.handle(Future.succeededFuture(dbResponseJson));
        } catch (IOException e) {
          logger.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
              Constants.DATABASE_ERROR);
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        logger.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
            Constants.DATABASE_ERROR);
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject errorJson = new JsonObject();
    logger.info("Inside countQuery<DatabaseService> block-------- " + request.toString());
    request.put(Constants.SEARCH, false);
    // if (!request.containsKey("instanceId")) {
    // errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION, "No instanceId
    // found");
    // handler.handle(Future.failedFuture(errorJson.toString()));
    // return null;
    // }
    /* Validate the Request */
    if (!request.containsKey(Constants.SEARCH_TYPE)) {
      errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
          Constants.NO_SEARCH_TYPE_FOUND);
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    Request elasticRequest = new Request("GET", Constants.CAT_TEST_COUNT_INDEX);
    JsonObject query = queryDecoder(request);
    if (query.containsKey("Error")) {
      logger.info("Query returned with an error");
      errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
          query.getString(Constants.ERROR));
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    logger.info("Query constructed: " + query.toString());
    elasticRequest.setJsonEntity(query.toString());
    client.performRequestAsync(elasticRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        logger.info("Successful DB request");
        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            handler.handle(Future.failedFuture("Status code is not 2xx"));
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          handler.handle(Future.succeededFuture(
              new JsonObject().put(Constants.COUNT, responseJson.getInteger(Constants.COUNT))));
        } catch (IOException e) {
          logger.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
              Constants.DATABASE_ERROR);
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        logger.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
            Constants.DATABASE_ERROR);
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService createItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Request checkExisting;
    JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = request.getString("id");
    errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.RESULTS,
        new JsonArray().add(new JsonObject().put(Constants.ID, id)
            .put(Constants.METHOD, Constants.INSERT).put(Constants.STATUS, Constants.FAILED)));
    checkExisting = new Request(Constants.REQUEST_GET, Constants.CAT_TEST_SEARCH_INDEX);
    checkQuery.put(Constants.SOURCE, "[\"\"]").put(Constants.QUERY_KEY,
        new JsonObject().put(Constants.TERM, new JsonObject().put(Constants.ID_KEYWORD, id)));
    logger.info("Query constructed: " + checkQuery.toString());
    checkExisting.setJsonEntity(checkQuery.toString());
    client.performRequestAsync(checkExisting, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        logger.info("Successful DB request");
        int statusCode = response.getStatusLine().getStatusCode();
        logger.info("status code: " + statusCode);
        if (statusCode != 200 && statusCode != 204) {
          handler.handle(Future.failedFuture("Status code is not 2xx"));
          return;
        }
        try {
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
              .getInteger(Constants.VALUE) > 0) {
            logger.info("Item already exists.");
            handler.handle(Future.failedFuture(errorJson.toString()));
            return;
          } else {
            Request createRequest = new Request(Constants.REQUEST_POST, Constants.CAT_DOC);
            createRequest.setJsonEntity(request.toString());
            client.performRequestAsync(createRequest, new ResponseListener() {
              @Override
              public void onSuccess(Response response) {
                int statusCode = response.getStatusLine().getStatusCode();
                logger.info("status code: " + statusCode);
                if (statusCode != 200 && statusCode != 201 && statusCode != 204) {
                  handler.handle(Future.failedFuture("Status code is not 2xx"));
                  return;
                }
                logger.info("Successful DB request: Item Created");
                JsonObject responseJson = new JsonObject();
                responseJson.put(Constants.STATUS, Constants.SUCCESS).put(Constants.RESULTS,
                    new JsonArray().add(new JsonObject().put(Constants.ID, id)
                        .put(Constants.METHOD, Constants.INSERT)
                        .put(Constants.STATUS, Constants.SUCCESS)));
                handler.handle(Future.succeededFuture(responseJson));
              }

              @Override
              public void onFailure(Exception e) {
                logger.info("DB request has failed. ERROR:\n");
                e.printStackTrace();
                /* Handle request error */
                handler.handle(Future.failedFuture(errorJson.toString()));
              }
            });
          }
        } catch (ParseException | IOException e) {
          logger.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        logger.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Request checkExisting;
    JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = request.getString("id");
    errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.RESULTS,
        new JsonArray().add(new JsonObject().put(Constants.ID, id)
            .put(Constants.METHOD, Constants.UPDATE).put(Constants.STATUS, Constants.FAILED)));
    checkExisting = new Request(Constants.REQUEST_GET, Constants.CAT_TEST_SEARCH_INDEX);
    checkQuery.put(Constants.SOURCE, "[\"\"]").put(Constants.QUERY_KEY,
        new JsonObject().put(Constants.TERM, new JsonObject().put(Constants.ID_KEYWORD, id)));
    logger.info("Query constructed: " + checkQuery.toString());
    checkExisting.setJsonEntity(checkQuery.toString());
    client.performRequestAsync(checkExisting, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        logger.info("Successful DB request");
        int statusCode = response.getStatusLine().getStatusCode();
        logger.info("status code: " + statusCode);
        if (statusCode != 200 && statusCode != 204) {
          handler.handle(Future.failedFuture("Status code is not 2xx"));
          return;
        }
        try {
          Request updateRequest;
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
              .getInteger(Constants.VALUE) == 0) {
            logger.info("Item Doesn't Exist in the Database");
            updateRequest = new Request(Constants.REQUEST_POST, Constants.CAT_DOC);
            logger.info("Creating New Item");
          } else {
            logger.info("Item found");
            String docId = responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS)
                .getJsonObject(0).getString(Constants.DOC_ID);
            updateRequest = new Request(Constants.REQUEST_PUT, Constants.CAT_DOC + "/" + docId);
          }
          updateRequest.setJsonEntity(request.toString());
          client.performRequestAsync(updateRequest, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
              int statusCode = response.getStatusLine().getStatusCode();
              logger.info("status code: " + statusCode);
              if (statusCode != 200 && statusCode != 204 && statusCode != 201) {
                handler.handle(Future.failedFuture("Status code is not 2xx"));
                return;
              }
              logger.info("Successful DB request: Item Updated");
              JsonObject responseJson = new JsonObject();
              responseJson.put(Constants.STATUS, Constants.SUCCESS).put(Constants.RESULTS,
                  new JsonArray().add(
                      new JsonObject().put(Constants.ID, id).put(Constants.METHOD, Constants.UPDATE)
                          .put(Constants.STATUS, Constants.SUCCESS)));
              handler.handle(Future.succeededFuture(responseJson));
            }

            @Override
            public void onFailure(Exception e) {
              logger.info("DB request has failed. ERROR:\n");
              e.printStackTrace();
              /* Handle request error */
              handler.handle(Future.failedFuture(errorJson.toString()));
            }
          });

        } catch (ParseException | IOException e) {
          logger.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        logger.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Request checkExisting;
    JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = request.getString("id");
    errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.RESULTS,
        new JsonArray().add(new JsonObject().put(Constants.ID, id)
            .put(Constants.METHOD, Constants.DELETE).put(Constants.STATUS, Constants.FAILED)));
    checkExisting = new Request(Constants.REQUEST_GET, Constants.CAT_TEST_SEARCH_INDEX);
    checkQuery.put(Constants.SOURCE, "[\"\"]").put(Constants.QUERY_KEY,
        new JsonObject().put(Constants.TERM, new JsonObject().put(Constants.ID_KEYWORD, id)));
    checkExisting.setJsonEntity(checkQuery.toString());
    client.performRequestAsync(checkExisting, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        int statusCode = response.getStatusLine().getStatusCode();
        logger.info("status code: " + statusCode);
        if (statusCode != 200 && statusCode != 204) {
          handler.handle(Future.failedFuture("Status code is not 2xx"));
          return;
        }
        logger.info("Successful DB request");
        try {
          Request updateRequest;
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          logger.info("\n\n\n Response is \n\n\n");
          logger.info(responseJson);
          if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
              .getInteger(Constants.VALUE) == 0) {
            logger.info("Item Doesn't exist");
            handler.handle(Future.failedFuture(errorJson.toString()));
            return;
          }
          logger.info("Item Found");
          String docId = responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS)
              .getJsonObject(0).getString(Constants.DOC_ID);
          updateRequest = new Request(Constants.REQUEST_DELETE, Constants.CAT_DOC + "/" + docId);
          updateRequest.setJsonEntity(request.toString());
          client.performRequestAsync(updateRequest, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
              int statusCode = response.getStatusLine().getStatusCode();
              logger.info("status code: " + statusCode);
              if (statusCode != 200 && statusCode != 204) {
                handler.handle(Future.failedFuture("Status code is not 2xx"));
                return;
              }
              logger.info("Successful DB request: Item Deleted");
              JsonObject responseJson = new JsonObject();
              responseJson.put(Constants.STATUS, Constants.SUCCESS).put(Constants.RESULTS,
                  new JsonArray().add(
                      new JsonObject().put(Constants.ID, id).put(Constants.METHOD, Constants.DELETE)
                          .put(Constants.STATUS, Constants.SUCCESS)));
              handler.handle(Future.succeededFuture(responseJson));
            }

            @Override
            public void onFailure(Exception e) {
              logger.info("DB request has failed. ERROR:\n");
              e.printStackTrace();
              /* Handle request error */
              handler.handle(Future.failedFuture(errorJson.toString()));
            }
          });

        } catch (ParseException | IOException e) {
          logger.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        logger.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use

    String result =
        "{ \"status\": \"success\", \"totalHits\": 100," + "\"limit\": 10, \"offset\": 100,"
            + "\"results\": [" + "{ \"id\": \"abc/123\", \"tags\": [ \"a\", \"b\"] } ] }";

    String errResult = " { \"status\": \"invalidValue\", \"results\": [] }";

    if (request.getString("id").contains("/")) {
      handler.handle(Future.succeededFuture(new JsonObject(result)));
    } else {
      handler.handle(Future.succeededFuture(new JsonObject(errResult)));
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTags(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listDomains(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"Pune\", \"Varanasi\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"server-1\", \"server-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listProviders(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"pr-1\", \"pr-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceGroups(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceGroupId>/resource */
    /* Initialize elastic clients and JsonObjects */
    Request elasticRequest;
    JsonObject errorJson = new JsonObject();

    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Construct an elastic client request with index to query */
    elasticRequest =
        new Request(Constants.REQUEST_GET, Constants.REL_API_SEARCH_INDEX + Constants.FILTER_PATH);

    /* Validating the request */
    if (request.containsKey(Constants.ID)
        && request.getString(Constants.RELATIONSHIP).equals(Constants.REL_RESOURCE)) {

      /* parsing resourceGroupId from the request */
      String resourceGroupId = request.getString(Constants.ID);

      /* Constructing db queries */
      boolObject.put(Constants.BOOL_KEY,
          new JsonObject().put(Constants.MUST_KEY,
              new JsonArray()
                  .add(new JsonObject().put(Constants.TERM,
                      new JsonObject().put(Constants.REL_RESOURCE_GRP.concat(Constants.KEYWORD_KEY),
                          resourceGroupId)))
                  .add(new JsonObject().put(Constants.TERM,
                      new JsonObject().put(Constants.TYPE_KEY.concat(Constants.KEYWORD_KEY),
                          Constants.ITEM_TYPE_RESOURCE)))));

      elasticQuery.put(Constants.QUERY_KEY, boolObject);

      logger.info("Query constructed: " + elasticQuery.toString());

      /* Set the elastic client with the query to perform */
      elasticRequest.setJsonEntity(elasticQuery.toString());

      /* Execute the query */
      client.performRequestAsync(elasticRequest, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {

          logger.info("Successful DB request");
          JsonArray dbResponse = new JsonArray();
          JsonObject dbResponseJson = new JsonObject();

          try {
            int statusCode = response.getStatusLine().getStatusCode();
            /* Validate the response */
            if (statusCode != 200 && statusCode != 204) {
              handler.handle(Future.failedFuture("Status code is not 2xx"));
              return;
            }

            JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));

            if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
                .getInteger(Constants.VALUE) == 0) {

              /* Constructing error response */
              errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                  Constants.EMPTY_RESPONSE);

              handler.handle(Future.failedFuture(errorJson.toString()));
              return;
            }

            JsonArray responseHits =
                responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS);

            /* Construct the client response, remove the _source field */
            for (Object json : responseHits) {
              JsonObject jsonTemp = (JsonObject) json;
              dbResponse.add(jsonTemp.getJsonObject(Constants.SOURCE));
            }

            dbResponseJson.put(Constants.STATUS, Constants.SUCCESS)
                .put(Constants.TOTAL_HITS, responseJson.getJsonObject(Constants.HITS)
                    .getJsonObject(Constants.TOTAL).getInteger(Constants.VALUE))
                .put(Constants.RESULT, dbResponse);
            /* Send the response */
            handler.handle(Future.succeededFuture(dbResponseJson));

          } catch (IOException e) {

            logger.info("DB ERROR: " + e.getMessage());

            /* Constructing error response */
            errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                Constants.DATABASE_ERROR);

            handler.handle(Future.failedFuture(errorJson.toString()));
          }
        }

        @Override
        public void onFailure(Exception e) {

          logger.info("DB request has failed. ERROR: " + e.getMessage());

          /* Constructing error response */
          errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
              Constants.DATABASE_ERROR);

          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceGroupRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceId>/resourceGroup */
    /* Initialize elastic clients and JsonObjects */
    Request elasticRequest;
    JsonObject errorJson = new JsonObject();

    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Construct an elastic client request with index to query */
    elasticRequest =
        new Request(Constants.REQUEST_GET, Constants.REL_API_SEARCH_INDEX + Constants.FILTER_PATH);

    /* Validating the request */
    if (request.containsKey(Constants.ID)
        && request.getString(Constants.RELATIONSHIP).equals(Constants.REL_RESOURCE_GRP)) {

      /* parsing resourceGroupId from the request ID */
      String resourceGroupId =
          StringUtils.substringBeforeLast(request.getString(Constants.ID), Constants.FORWARD_SLASH);

      boolObject.put(Constants.BOOL_KEY,
          new JsonObject().put(Constants.MUST_KEY,
              new JsonArray()
                  .add(new JsonObject().put(Constants.TERM,
                      new JsonObject().put(Constants.ID_KEYWORD, resourceGroupId)))
                  .add(new JsonObject().put(Constants.TERM,
                      new JsonObject().put(Constants.TYPE_KEY.concat(Constants.KEYWORD_KEY),
                          Constants.ITEM_TYPE_RESOURCE_GROUP)))));

      elasticQuery.put(Constants.QUERY_KEY, boolObject);

      logger.info("Query constructed: " + elasticQuery.toString());

      /* Set the elastic client with the query to perform */
      elasticRequest.setJsonEntity(elasticQuery.toString());

      /* Execute the query */
      client.performRequestAsync(elasticRequest, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {

          logger.info("Successful DB request");
          JsonArray dbResponse = new JsonArray();
          JsonObject dbResponseJson = new JsonObject();

          try {
            int statusCode = response.getStatusLine().getStatusCode();
            /* Validate the response */
            if (statusCode != 200 && statusCode != 204) {
              handler.handle(Future.failedFuture("Status code is not 2xx"));
              return;
            }

            JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));

            if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
                .getInteger(Constants.VALUE) == 0) {

              /* Constructing error response */
              errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                  Constants.EMPTY_RESPONSE);

              handler.handle(Future.failedFuture(errorJson.toString()));
              return;
            }

            JsonArray responseHits =
                responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS);

            /* Construct the client response, remove the _source field */
            for (Object json : responseHits) {
              JsonObject jsonTemp = (JsonObject) json;
              dbResponse.add(jsonTemp.getJsonObject(Constants.SOURCE));
            }

            dbResponseJson.put(Constants.STATUS, Constants.SUCCESS)
                .put(Constants.TOTAL_HITS, responseJson.getJsonObject(Constants.HITS)
                    .getJsonObject(Constants.TOTAL).getInteger(Constants.VALUE))
                .put(Constants.RESULT, dbResponse);
            /* Send the response */
            handler.handle(Future.succeededFuture(dbResponseJson));

          } catch (IOException e) {

            logger.info("DB ERROR: " + e.getMessage());

            /* Constructing error response */
            errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                Constants.DATABASE_ERROR);

            handler.handle(Future.failedFuture(errorJson.toString()));
          }
        }

        @Override
        public void onFailure(Exception e) {

          logger.info("DB request has failed. ERROR: " + e.getMessage());

          /* Constructing error response */
          errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
              Constants.DATABASE_ERROR);

          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listProviderRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceId>/provider */
    /* Initialize elastic clients and JsonObjects */
    Request elasticRequest;
    JsonObject errorJson = new JsonObject();

    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Construct an elastic client request with index to query */
    elasticRequest =
        new Request(Constants.REQUEST_GET, Constants.REL_API_SEARCH_INDEX + Constants.FILTER_PATH);

    /* Validating the request */
    if (request.containsKey(Constants.ID)
        && request.getString(Constants.RELATIONSHIP).equals(Constants.REL_PROVIDER)) {

      /* parsing id from the request */
      String id = request.getString(Constants.ID);

      /* parsing providerId from the request */
      String providerId = StringUtils.substring(id, 0, id.indexOf("/", id.indexOf("/") + 1));

      boolObject.put(Constants.BOOL_KEY, new JsonObject().put(Constants.MUST_KEY, new JsonArray()
          .add(new JsonObject().put(Constants.TERM,
              new JsonObject().put(Constants.ID_KEYWORD, providerId)))
          .add(new JsonObject().put(Constants.TERM, new JsonObject().put(
              Constants.TYPE_KEY.concat(Constants.KEYWORD_KEY), Constants.ITEM_TYPE_PROVIDER)))));

      elasticQuery.put(Constants.QUERY_KEY, boolObject);

      logger.info("Query constructed: " + elasticQuery.toString());

      /* Set the elastic client with the query to perform */
      elasticRequest.setJsonEntity(elasticQuery.toString());

      /* Execute the query */
      client.performRequestAsync(elasticRequest, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {

          logger.info("Successful DB request");
          JsonArray dbResponse = new JsonArray();
          JsonObject dbResponseJson = new JsonObject();

          try {
            int statusCode = response.getStatusLine().getStatusCode();
            /* Validate the response */
            if (statusCode != 200 && statusCode != 204) {
              handler.handle(Future.failedFuture("Status code is not 2xx"));
              return;
            }

            JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));

            if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
                .getInteger(Constants.VALUE) == 0) {

              /* Constructing error response */
              errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                  Constants.EMPTY_RESPONSE);

              handler.handle(Future.failedFuture(errorJson.toString()));
              return;
            }

            JsonArray responseHits =
                responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS);

            /* Construct the client response, remove the _source field */
            for (Object json : responseHits) {
              JsonObject jsonTemp = (JsonObject) json;
              dbResponse.add(jsonTemp.getJsonObject(Constants.SOURCE));
            }

            dbResponseJson.put(Constants.STATUS, Constants.SUCCESS)
                .put(Constants.TOTAL_HITS, responseJson.getJsonObject(Constants.HITS)
                    .getJsonObject(Constants.TOTAL).getInteger(Constants.VALUE))
                .put(Constants.RESULT, dbResponse);
            /* Send the response */
            handler.handle(Future.succeededFuture(dbResponseJson));

          } catch (IOException e) {

            logger.info("DB ERROR: " + e.getMessage());

            /* Constructing error response */
            errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                Constants.DATABASE_ERROR);

            handler.handle(Future.failedFuture(errorJson.toString()));
          }
        }

        @Override
        public void onFailure(Exception e) {

          logger.info("DB request has failed. ERROR: " + e.getMessage());

          /* Constructing error response */
          errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
              Constants.DATABASE_ERROR);

          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServerRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceId>/resourceServer */
    /* Initialize elastic clients and JsonObjects */
    Request elasticRequest;
    JsonObject errorJson = new JsonObject();

    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Construct an elastic client request with index to query */
    elasticRequest =
        new Request(Constants.REQUEST_GET, Constants.REL_API_SEARCH_INDEX + Constants.FILTER_PATH);

    /* Validating the request */
    if (request.containsKey(Constants.ID)
        && request.getString(Constants.RELATIONSHIP).equals(Constants.REL_RESOURCE_SVR)) {

      /* parsing id from the request */
      String[] id = request.getString(Constants.ID).split(Constants.FORWARD_SLASH);

      boolObject.put(Constants.BOOL_KEY, new JsonObject().put(Constants.MUST_KEY, new JsonArray()
          .add(new JsonObject().put(Constants.MATCH_KEY, new JsonObject().put(Constants.ID, id[0])))
          .add(new JsonObject().put(Constants.MATCH_KEY, new JsonObject().put(Constants.ID, id[2])))
          .add(new JsonObject().put(Constants.TERM,
              new JsonObject().put(Constants.TYPE_KEY.concat(Constants.KEYWORD_KEY),
                  Constants.ITEM_TYPE_RESOURCE_SERVER)))));

      elasticQuery.put(Constants.QUERY_KEY, boolObject);

      logger.info("Query constructed: " + elasticQuery.toString());

      /* Set the elastic client with the query to perform */
      elasticRequest.setJsonEntity(elasticQuery.toString());

      /* Execute the query */
      client.performRequestAsync(elasticRequest, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {

          logger.info("Successful DB request");
          JsonArray dbResponse = new JsonArray();
          JsonObject dbResponseJson = new JsonObject();

          try {
            int statusCode = response.getStatusLine().getStatusCode();
            /* Validate the response */
            if (statusCode != 200 && statusCode != 204) {
              handler.handle(Future.failedFuture("Status code is not 2xx"));
              return;
            }

            JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));

            if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
                .getInteger(Constants.VALUE) == 0) {

              /* Constructing error response */
              errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                  Constants.EMPTY_RESPONSE);

              handler.handle(Future.failedFuture(errorJson.toString()));
              return;
            }

            JsonArray responseHits =
                responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS);

            /* Construct the client response, remove the _source field */
            for (Object json : responseHits) {
              JsonObject jsonTemp = (JsonObject) json;
              dbResponse.add(jsonTemp.getJsonObject(Constants.SOURCE));
            }

            dbResponseJson.put(Constants.STATUS, Constants.SUCCESS)
                .put(Constants.TOTAL_HITS, responseJson.getJsonObject(Constants.HITS)
                    .getJsonObject(Constants.TOTAL).getInteger(Constants.VALUE))
                .put(Constants.RESULT, dbResponse);
            /* Send the response */
            handler.handle(Future.succeededFuture(dbResponseJson));

          } catch (IOException e) {

            logger.info("DB ERROR: " + e.getMessage());

            /* Constructing error response */
            errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                Constants.DATABASE_ERROR);

            handler.handle(Future.failedFuture(errorJson.toString()));
          }
        }

        @Override
        public void onFailure(Exception e) {

          logger.info("DB request has failed. ERROR: " + e.getMessage());

          /* Constructing error response */
          errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
              Constants.DATABASE_ERROR);

          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTypes(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceId or resourceGroupId>/resourceServer */
    /* Initialize elastic clients and JsonObjects */
    Request elasticRequest;
    JsonObject errorJson = new JsonObject();

    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Construct an elastic client request with index to query */
    elasticRequest =
        new Request(Constants.REQUEST_GET, Constants.REL_API_SEARCH_INDEX + Constants.FILTER_PATH);

    /* Validating the request */
    if (request.containsKey(Constants.ID)
        && request.getString(Constants.RELATIONSHIP).equals(Constants.TYPE_KEY)) {

      /* parsing id from the request */
      String itemId = request.getString(Constants.ID);

      boolObject.put(Constants.BOOL_KEY,
          new JsonObject().put(Constants.MUST_KEY, new JsonArray().add(new JsonObject()
              .put(Constants.TERM, new JsonObject().put(Constants.ID_KEYWORD, itemId)))));

      elasticQuery.put(Constants.QUERY_KEY, boolObject).put(Constants.SOURCE,
          request.getString(Constants.RELATIONSHIP));

      logger.info("Query constructed: " + elasticQuery.toString());

      /* Set the elastic client with the query to perform */
      elasticRequest.setJsonEntity(elasticQuery.toString());

      /* Execute the query */
      client.performRequestAsync(elasticRequest, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {

          logger.info("Successful DB request");
          JsonArray dbResponse = new JsonArray();
          JsonObject dbResponseJson = new JsonObject();

          try {
            int statusCode = response.getStatusLine().getStatusCode();
            /* Validate the response */
            if (statusCode != 200 && statusCode != 204) {
              handler.handle(Future.failedFuture("Status code is not 2xx"));
              return;
            }

            JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));

            if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
                .getInteger(Constants.VALUE) == 0) {

              /* Constructing error response */
              errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                  Constants.EMPTY_RESPONSE);

              handler.handle(Future.failedFuture(errorJson.toString()));
              return;
            }

            JsonArray responseHits =
                responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS);

            /* Construct the client response, remove the _source field */
            for (Object json : responseHits) {
              JsonObject jsonTemp = (JsonObject) json;
              dbResponse.add(jsonTemp.getJsonObject(Constants.SOURCE));
            }

            dbResponseJson.put(Constants.STATUS, Constants.SUCCESS)
                .put(Constants.TOTAL_HITS, responseJson.getJsonObject(Constants.HITS)
                    .getJsonObject(Constants.TOTAL).getInteger(Constants.VALUE))
                .put(Constants.RESULT, dbResponse);
            /* Send the response */
            handler.handle(Future.succeededFuture(dbResponseJson));

          } catch (IOException e) {

            logger.info("DB ERROR: " + e.getMessage());

            /* Constructing error response */
            errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                Constants.DATABASE_ERROR);

            handler.handle(Future.failedFuture(errorJson.toString()));
          }
        }

        @Override
        public void onFailure(Exception e) {

          logger.info("DB request has failed. ERROR: " + e.getMessage());

          /* Constructing error response */
          errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
              Constants.DATABASE_ERROR);

          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService getCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "\"status\": \"success\",\n" + "\"results\": [{\n"
        + "\"__instance-id\" : \"ui-test.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"PSCDCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"__instance-id\" : \"covid-19.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"COVID-19\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"__instance-id\" : \"pudx.catalogue.iudx.org.in\",\n" + "\"configurations\" " + ": {\n"
        + "\"smart_city_name\" : \"PSCDCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"__instance-id\" : \"varanasi.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"VSCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 25.3176, 82.9739 ]\n" + "}\n" + "}]}";

    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService setCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instanceID\": \"ui-test.iudx.org.in\",\n"
        + "            \"configurations\": {\n"
        + "                \"smart_city_name\": \"PSCDCL\",\n"
        + "                \"map_default_view_lat_lng\": [\n" + "                    18.5644"
        + ",\n" + "                    73.7858\n" + "                ]\n" + "            }\n"
        + "        },\n" + "        {\n" + "            \"instanceID\": \"covid-19.iudx.org.i"
        + "n\"," + "\n" + "            \"configurations\": {\n"
        + "                \"smart_city_name\": \"COVID-19\",\n"
        + "                \"map_default_view_lat_lng\": [\n" + "                    18.5644,"
        + "\n" + "                    73.7858\n" + "                ]\n" + "            }\n"
        + "        },\n" + "        {\n"
        + "            \"instanceID\": \"pudx.catalogue.iudx.org.in\",\n"
        + "            \"configurations\": {\n"
        + "                \"smart_city_name\": \"PSCDCL\",\n"
        + "                \"map_default_view_lat_lng\": [\n" + "                    18.5644,"
        + "\n" + "                    73.7858\n" + "                ]\n" + "            }\n"
        + "        },\n" + "        {\n" + "            \"instanceID\": \"varanasi.iudx.org.i"
        + "n\",\n" + "            \"configurations\": {\n"
        + "                \"smart_city_name\": \"VSC" + "L\",\n"
        + "                \"map_default_view_lat_lng\": [\n" + "                    25.3176,\n"
        + "                    82.9739\n" + "                ]\n" + "            }\n"
        + "        }\n" + "    ]\n" + "}";

    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateCities(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "\"status\": \"success\",\n" + "\"results\": [\n" + "{\n"
        + "\"instanceID\" : \"ui-test.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"PSCDCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"instanceID\" : \"covid-19.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"COVID-19\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"instanceID\" : \"pudx.catalogue.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"PSCDCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 18.5644, 73.7858 ]\n" + "}\n" + "}, {\n"
        + "\"instanceID\" : \"varanasi.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_name\" : \"VSCL\",\n"
        + "\"map_default_view_lat_lng\" : [ 25.3176, 82.9739 ]\n" + "}\n" + "}]}";

    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService getConfig(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "\"status\": \"success\",\n" + "\"results\": [{\n"
        + "\"__instance-id\" : \"varanasi.iudx.org.in\",\n" + "\"configurations\" : {\n"
        + "\"smart_city_iudx_logo\" : \"../assets/img/iudx_varanasi.jpeg\",\n"
        + "\"smart_city_name\" : \"VSCL\",\n" + "\"smart_city_url\" : \"#\",\n"
        + "\"resoure_server_base_URL\" : \"https://rs.varanasi.iudx.org.in/resource-server/vscl/v1"
        + "\",\n" + "\"auth_base_URL\" : \"https://auth.iudx.org.in/auth/v1\",\n"
        + "\"api_docs_link\" : \"https://apidocs.iudx.org.in\",\n"
        + "\"resource_server_group_head\" : \"urn:iudx-catalogue-varanasi:\",\n"
        + "\"provider_head\" : \"urn:iudx-catalogue-varanasi:\",\n"
        + "\"map_default_view_lat_lng\" : [ 25.3176, 82.9739 ],\n"
        + "\"map_default_lat_lng_name\" : \"VSCL Office\",\n" + "\"map_default_zoom\" : 12.0,\n"
        + "\"cat_base_URL\" : \"https://varanasi.iudx.org.in/catalogue/v1\"\n" + "},\n"
        + "\"legends\" : {\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-swm-bins\" : \"https://image.flaticon.com/icons/svg/26"
        + "36/2636439.svg\",\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-aqm\" : \"https://image.flaticon.com/icons/svg/1808/"
        + "180" + "8701.svg\",\n" + "\"rs.varanasi.iudx.org.in/varanasi-swm-vehicles\" : \"#\",\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-citizen-app\" : \"#\",\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-iudx-gis\" : \"#\",\n"
        + "\"rs.varanasi.iudx.org.in/varanasi-swm-workers\" : \"#\"\n" + "},\n"
        + "\"global_configuration\" : {\n" + "\"icon_attribution\" : {\n" + "\"author\" : [ {\n"
        + "\"freepik\" : \"https://www.flaticon.com/authors/freepik\"\n" + "}, {\n"
        + "\"smashicons\" : \"https://www.flaticon.com/authors/smashicons\"\n" + "}, {\n"
        + "\"flat-icons\" : \"https://www.flaticon.com/authors/flat-icons\"\n" + "}, {\n"
        + "\"itim2101\" : \"https://www.flaticon.com/authors/itim2101\"\n" + "} ],\n"
        + "\"site\" : \"flaticon.com\",\n" + "\"site_link\" : \"https://flaticon.com\"\n" + "}\n"
        + "}\n" + "}]}";

    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateConfig(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instance-id\": \"<iudx-instance>:id\",\n"
        + "            \"method\": \"update\",\n" + "            \"status\": \"success\"\n"
        + "        }\n" + "    ]\n" + "}";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService setConfig(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instance-id\": \"<iudx-instance>:id\",\n"
        + "            \"method\": \"insert\",\n" + "            \"status\": \"success\"\n"
        + "        }\n" + "    ]\n" + "}";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService deleteConfig(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instance-id\": \"<iudx-instance>:id\",\n"
        + "            \"method\": \"delete\",\n" + "            \"status\": \"success\"\n"
        + "        }\n" + "    ]\n" + "}";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService appendConfig(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO Auto-generated method stub

    String result = "{\n" + "    \"status\": \"success\",\n" + "    \"results\": [\n"
        + "        {\n" + "            \"instance-id\": \"<iudx-instance>:id\",\n"
        + "            \"method\": \"patch\",\n" + "            \"status\": \"success\"\n"
        + "        }\n" + "    ]\n" + "}";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceId or resourceGroupId>/resourceServer */
    /* Initialize elastic clients and JsonObjects */
    Request elasticRequest;
    JsonObject errorJson = new JsonObject();

    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Construct an elastic client request with index to query */
    elasticRequest =
        new Request(Constants.REQUEST_GET, Constants.REL_API_SEARCH_INDEX + Constants.FILTER_PATH);

    /* Validating the request */
    if (request.containsKey(Constants.ID)
        && request.getString(Constants.RELATIONSHIP).equals(Constants.TYPE_KEY)) {

      /* parsing id from the request */
      String itemId = request.getString(Constants.ID);

      boolObject.put(Constants.BOOL_KEY,
          new JsonObject().put(Constants.MUST_KEY, new JsonArray().add(new JsonObject()
              .put(Constants.TERM, new JsonObject().put(Constants.ID_KEYWORD, itemId)))));

      elasticQuery.put(Constants.QUERY_KEY, boolObject).put(Constants.SOURCE,
          request.getString(Constants.RELATIONSHIP));

      logger.info("Query constructed: " + elasticQuery.toString());

      /* Set the elastic client with the query to perform */
      elasticRequest.setJsonEntity(elasticQuery.toString());

      /* Execute the query */
      client.performRequestAsync(elasticRequest, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {

          logger.info("Successful DB request");
          JsonArray dbResponse = new JsonArray();
          JsonObject dbResponseJson = new JsonObject();

          try {
            int statusCode = response.getStatusLine().getStatusCode();
            /* Validate the response */
            if (statusCode != 200 && statusCode != 204) {
              handler.handle(Future.failedFuture("Status code is not 2xx"));
              return;
            }

            JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));

            if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
                .getInteger(Constants.VALUE) == 0) {

              /* Constructing error response */
              errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                  Constants.EMPTY_RESPONSE);

              handler.handle(Future.failedFuture(errorJson.toString()));
              return;
            }

            JsonArray responseHits =
                responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS);

            /* Construct the client response, remove the _source field */
            for (Object json : responseHits) {
              JsonObject jsonTemp = (JsonObject) json;
              dbResponse.add(jsonTemp.getJsonObject(Constants.SOURCE));
            }

            dbResponseJson.put(Constants.STATUS, Constants.SUCCESS)
                .put(Constants.TOTAL_HITS, responseJson.getJsonObject(Constants.HITS)
                    .getJsonObject(Constants.TOTAL).getInteger(Constants.VALUE))
                .put(Constants.RESULT, dbResponse);
            /* Send the response */
            handler.handle(Future.succeededFuture(dbResponseJson));

          } catch (IOException e) {

            logger.info("DB ERROR: " + e.getMessage());

            /* Constructing error response */
            errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                Constants.DATABASE_ERROR);

            handler.handle(Future.failedFuture(errorJson.toString()));
          }
        }

        @Override
        public void onFailure(Exception e) {

          logger.info("DB request has failed. ERROR: " + e.getMessage());

          /* Constructing error response */
          errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
              Constants.DATABASE_ERROR);

          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * Decodes and constructs ElasticSearch Search/Count query based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public JsonObject queryDecoder(JsonObject request) {
    String searchType = request.getString(Constants.SEARCH_TYPE);
    JsonObject elasticQuery = new JsonObject();
    Boolean match = false;
    JsonObject boolObject = new JsonObject().put(Constants.BOOL_KEY, new JsonObject());
    /* TODO: Pagination for large result set */
    if (request.getBoolean(Constants.SEARCH)) {
      elasticQuery.put(Constants.SIZE_KEY, 10);
    }
    // Will be used for multi-tenancy
    // String instanceId = request.getString("instanceId");
    JsonArray filterQuery = new JsonArray();
    JsonArray mustQuery = new JsonArray();
    // Will be used for multi-tenancy
    // JsonObject termQuery =
    // new JsonObject().put("term", new JsonObject()
    // .put(INSTANCE_ID_KEY + ".keyword", instanceId));
    // filterQuery.add(termQuery);

    /* Handle the search type */
    if (searchType.matches(Constants.GEOSEARCH_REGEX)) {
      logger.info("In geoSearch block---------");
      match = true;
      JsonObject shapeJson = new JsonObject();
      JsonObject geoSearch = new JsonObject();
      String relation;
      JsonArray coordinates;
      /* Construct the search query */
      if (request.containsKey(Constants.GEOMETRY)
          && request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.POINT)
          && request.containsKey(Constants.GEORELATION)
          && request.containsKey(Constants.COORDINATES_KEY)
          && request.containsKey(Constants.GEOPROPERTY)
          && request.containsKey(Constants.MAX_DISTANCE)) {
        /* Construct the query for Circle */
        coordinates = request.getJsonArray(Constants.COORDINATES_KEY);
        int radius = request.getInteger(Constants.MAX_DISTANCE);
        // int radius = Integer.parseInt(request.getString(Constants.MAX_DISTANCE));
        relation = request.getString(Constants.GEORELATION);
        shapeJson
            .put(Constants.SHAPE_KEY,
                new JsonObject().put(Constants.TYPE_KEY, Constants.GEO_CIRCLE)
                    .put(Constants.COORDINATES_KEY, coordinates)
                    .put(Constants.GEO_RADIUS, radius + Constants.DISTANCE_IN_METERS))
            .put(Constants.GEO_RELATION_KEY, relation);
      } else if (request.containsKey(Constants.GEOMETRY)
          && (request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.POLYGON)
              || request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.LINESTRING))
          && request.containsKey(Constants.GEORELATION)
          && request.containsKey(Constants.COORDINATES_KEY)
          && request.containsKey(Constants.GEOPROPERTY)) {
        /* Construct the query for Line String, Polygon */
        String geometry = request.getString(Constants.GEOMETRY);
        relation = request.getString(Constants.GEORELATION);
        coordinates = request.getJsonArray(Constants.COORDINATES_KEY);
        int length = coordinates.getJsonArray(0).size();
        if (geometry.equalsIgnoreCase(Constants.POLYGON)
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
          return new JsonObject().put(Constants.ERROR, Constants.ERROR_INVALID_COORDINATE_POLYGON);
        }
        shapeJson
            .put(Constants.SHAPE_KEY, new JsonObject().put(Constants.TYPE_KEY, geometry)
                .put(Constants.COORDINATES_KEY, coordinates))
            .put(Constants.GEO_RELATION_KEY, relation);
      } else if (request.containsKey(Constants.GEOMETRY)
          && request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.BBOX)
          && request.containsKey(Constants.GEORELATION)
          && request.containsKey(Constants.COORDINATES_KEY)
          && request.containsKey(Constants.GEOPROPERTY)) {
        /* Construct the query for BBOX */
        relation = request.getString(Constants.GEORELATION);
        coordinates = request.getJsonArray(Constants.COORDINATES_KEY);
        shapeJson = new JsonObject();
        shapeJson
            .put(Constants.SHAPE_KEY,
                new JsonObject().put(Constants.TYPE_KEY, Constants.GEO_BBOX)
                    .put(Constants.COORDINATES_KEY, coordinates))
            .put(Constants.GEO_RELATION_KEY, relation);

      } else {
        return new JsonObject().put(Constants.ERROR, Constants.ERROR_INVALID_GEO_PARAMETER);
      }
      geoSearch.put(Constants.GEO_SHAPE_KEY, new JsonObject().put(Constants.GEO_KEY, shapeJson));
      filterQuery.add(geoSearch);

    }

    /* Construct the query for text based search */
    if (searchType.matches(Constants.TEXTSEARCH_REGEX)) {
      logger.info("Text search block");

      match = true;
      /* validating tag search attributes */
      if (request.containsKey(Constants.Q_KEY) && !request.getString(Constants.Q_KEY).isBlank()) {

        /* fetching values from request */
        String textAttr = request.getString(Constants.Q_KEY);

        /* constructing db queries */
        mustQuery.add(new JsonObject().put(Constants.STRING_QUERY_KEY,
            new JsonObject().put(Constants.QUERY_KEY, textAttr)));
      }
    }

    /* Construct the query for attribute based search */
    if (searchType.matches(Constants.ATTRIBUTE_SEARCH_REGEX)) {
      logger.info("Attribute search block");

      match = true;

      /* validating tag search attributes */
      if (request.containsKey(Constants.PROPERTY)
          && !request.getJsonArray(Constants.PROPERTY).isEmpty()
          && request.containsKey(Constants.VALUE)
          && !request.getJsonArray(Constants.VALUE).isEmpty()) {

        /* fetching values from request */
        JsonArray propertyAttrs = request.getJsonArray(Constants.PROPERTY);
        JsonArray valueAttrs = request.getJsonArray(Constants.VALUE);

        /* For attribute property and values search */
        if (propertyAttrs.size() == valueAttrs.size()) {

          /* Mapping and constructing the value attributes with the property attributes for query */
          for (int i = 0; i < valueAttrs.size(); i++) {
            JsonObject boolQuery = new JsonObject();
            JsonArray shouldQuery = new JsonArray();
            JsonArray valueArray = valueAttrs.getJsonArray(i);

            for (int j = 0; j < valueArray.size(); j++) {
              JsonObject matchQuery = new JsonObject();

              /* Attribute related queries using "match" and without the ".keyword" */
              if (propertyAttrs.getString(i).equals(Constants.TAGS)
                  || propertyAttrs.getString(i).equals(Constants.DESCRIPTION_ATTR)
                  || propertyAttrs.getString(i).startsWith(Constants.LOCATION)) {

                matchQuery.put(propertyAttrs.getString(i), valueArray.getString(j));
                shouldQuery.add(new JsonObject().put(Constants.MATCH_KEY, matchQuery));

                /* Attribute related queries using "match" and with the ".keyword" */
              } else {
                /* checking keyword in the query paramters */
                if (propertyAttrs.getString(i).endsWith(Constants.KEYWORD_KEY)) {
                  matchQuery.put(propertyAttrs.getString(i), valueArray.getString(j));

                } else {

                  /* add keyword if not avaialble */
                  matchQuery.put(propertyAttrs.getString(i).concat(Constants.KEYWORD_KEY),
                      valueArray.getString(j));
                }
                shouldQuery.add(new JsonObject().put(Constants.MATCH_KEY, matchQuery));
              }
            }
            mustQuery.add(new JsonObject().put(Constants.BOOL_KEY,
                boolQuery.put(Constants.SHOULD_KEY, shouldQuery)));
          }
        } else {
          return new JsonObject().put(Constants.ERROR, Constants.ERROR_INVALID_PARAMETER);
        }
      }
    }

    /* checking the requests for limit attribute */
    if (request.containsKey(Constants.LIMIT)) {
      Integer sizeFilter = request.getInteger(Constants.LIMIT);
      elasticQuery.put(Constants.SIZE, sizeFilter);
    }

    /* checking the requests for offset attribute */
    if (request.containsKey(Constants.OFFSET)) {
      Integer offsetFilter = request.getInteger(Constants.OFFSET);
      elasticQuery.put(Constants.FROM, offsetFilter);
    }

    if (searchType.matches(Constants.RESPONSE_FILTER_REGEX)) {
      /* Construct the filter for response */
      logger.info("In responseFilter block---------");
      match = true;
      if (!request.getBoolean(Constants.SEARCH)) {
        return new JsonObject().put("Error", Constants.COUNT_UNSUPPORTED);
      }
      if (request.containsKey(Constants.ATTRIBUTE)) {
        JsonArray sourceFilter = request.getJsonArray(Constants.ATTRIBUTE);
        elasticQuery.put(Constants.SOURCE, sourceFilter);
      } else if (request.containsKey(Constants.FILTER_KEY)) {
        JsonArray sourceFilter = request.getJsonArray(Constants.FILTER_KEY);
        elasticQuery.put(Constants.SOURCE, sourceFilter);
        elasticQuery.put(Constants.SOURCE, sourceFilter);
      } else {
        return new JsonObject().put(Constants.ERROR, Constants.ERROR_INVALID_RESPONSE_FILTER);
      }
    }

    if (!match) {
      return new JsonObject().put("Error", Constants.INVALID_SEARCH);
    } else {
      /* return fully formed elastic query */
      boolObject.getJsonObject(Constants.BOOL_KEY).put(Constants.FILTER_KEY, filterQuery)
          .put(Constants.MUST_KEY, mustQuery);
      return elasticQuery.put(Constants.QUERY_KEY, boolObject);
    }
  }
}

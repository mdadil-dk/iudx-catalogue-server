package iudx.catalogue.server.util;

import java.util.ArrayList;
import java.util.Arrays;

public class Constants {

  /** Class Names */
  public static final String GEOCODING_PACKAGE_NAME = "iudx.catalogue.server.geocoding";
  public static final String NLPSEARCH_PACKAGE_NAME = "iudx.catalogue.server.nlpsearch";


  /** Service Addresses */
  public static final String DATABASE_SERVICE_ADDRESS = "iudx.catalogue.database.service";
  public static final String AUTH_SERVICE_ADDRESS = "iudx.catalogue.authentication.service";
  public static final String VALIDATION_SERVICE_ADDRESS = "iudx.catalogue.validator.service";
  public static final String GEOCODING_SERVICE_ADDRESS = "iudx.catalogue.geocoding.service";
  public static final String NLP_SERVICE_ADDRESS = "iudx.catalogue.nlpsearch.service";

  /** General */

  public static final String CONFIG_FILE = "config.properties";
  public static final String OPTIONAL_MODULES = "optionalModules";
  public static final String IS_SSL = "ssl";
  public static final String PORT = "port";
  public static final String KEYSTORE_PATH = "keystorePath";
  public static final String KEYSTORE_PASSWORD = "keystorePassword";
  public static final String DATABASE_IP = "databaseIP";
  public static final String DATABASE_PORT = "databasePort";
  public static final String DATABASE_UNAME = "databaseUser";
  public static final String DOC_INDEX = "docIndex";
  public static final String DATABASE_PASSWD = "databasePassword";
  public static final String SOURCE = "_source";

  /** Item type */
  public static final String RELATIONSHIP = "relationship";
  public static final String RESOURCE = "resource";
  public static final String RESOURCE_GRP = "resourceGroup";
  public static final String RESOURCE_SVR = "resourceServer";
  public static final String PROVIDER = "provider";
  public static final String TYPE = "type";

  /** Item types */
  public static final String ITEM_TYPE_RESOURCE = "iudx:Resource";
  public static final String ITEM_TYPE_RESOURCE_GROUP = "iudx:ResourceGroup";
  public static final String ITEM_TYPE_RESOURCE_SERVER = "iudx:ResourceServer";
  public static final String ITEM_TYPE_PROVIDER = "iudx:Provider";
  public static final String ITEM_TYPE_INSTANCE = "iudx:Instance";

  public static final ArrayList<String> ITEM_TYPES =
      new ArrayList<String>(Arrays.asList(ITEM_TYPE_RESOURCE, ITEM_TYPE_RESOURCE_GROUP,
          ITEM_TYPE_RESOURCE_SERVER, ITEM_TYPE_PROVIDER));

  public static final String AGGREGATIONS = "aggregations";
  public static final String INSTANCE = "instance";
  public static final String BUCKETS = "buckets";
  public static final String ID = "id";
  public static final String ITEM_TYPE = "itemType";

  public static final String SUCCESS = "success";
  public static final String PROPERTY = "property";
  public static final String VALUE = "value";

  /** GeoRels */
  public static final String GEOREL_WITHIN = "within";
  public static final String GEOREL_NEAR = "near";
  public static final String GEOREL_COVERED_BY = "coveredBy";
  public static final String GEOREL_INTERSECTS = "intersects";
  public static final String GEOREL_EQUALS = "equals";
  public static final String GEOREL_DISJOINT = "disjoint";

  /** Geometries */
  public static final String BBOX = "bbox";
  public static final String GEOMETRY = "geometry";
  public static final String GEOPROPERTY = "geoproperty";
  public static final String GEORELATION = "georel";
  public static final String INTERSECTS = "intersects";
  public static final String LINESTRING = "LineString";
  public static final String LOCATION = "location";
  public static final String MAX_DISTANCE = "maxDistance";
  public static final String POINT = "Point";
  public static final String POLYGON = "Polygon";
  public static final String COORDINATES = "coordinates";
  public static final String Q_VALUE = "q";
  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";

  /** SearchTypes */
  public static final String SEARCH_TYPE = "searchType";
  public static final String SEARCH_TYPE_GEO = "geoSearch_";
  public static final String SEARCH_TYPE_TEXT = "textSearch_";
  public static final String SEARCH_TYPE_ATTRIBUTE = "attributeSearch_";
  public static final String SEARCH_TYPE_TAGS = "tagsSearch_";
  public static final String RESPONSE_FILTER = "responseFilter_";

  public static final String MESSAGE = "message";
  public static final String RESULTS = "results";
  public static final String METHOD = "method";
  public static final String STATUS = "status";
  public static final String FAILED = "failed";
  public static final String ERROR = "error";
  public static final String DESC = "description";

  /** DB Query */
  public static final String TOTAL_HITS = "totalHits";
  public static final String QUERY_KEY = "query";
  public static final String HITS = "hits";
  public static final String TOTAL = "total";
  public static final String TERM = "term";
  public static final String NAME = "name";
  public static final String FILTER = "filter";
  public static final String TAGS = "tags";

  /** HTTP Methods */
  public static final String REQUEST_GET = "GET";
  public static final String REQUEST_POST = "POST";
  public static final String REQUEST_PUT = "PUT";
  public static final String REQUEST_DELETE = "DELETE";

  /** Error Messages */
  public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
  public static final String DATABASE_ERROR = "DB Error. Check logs for more information";

  /** Operation type */
  public static final String INSERT = "insert";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";

  /** Limits/Constraints */
  public static final long COORDINATES_SIZE = 10;
  public static final int COORDINATES_PRECISION = 6;
  public static final int STRING_SIZE = 100;
  public static final int PROPERTY_SIZE = 4;
  public static final int VALUE_SIZE = 4;
  public static final int FILTER_VALUE_SIZE = 10;
  public static final int ID_SIZE = 512;
  public static final int INSTANCE_SIZE = 100;
  public static final int FILTER_PAGINATION_SIZE = 10000;
  public static final int OFFSET_PAGINATION_SIZE = 9999;
  public static final int MAX_RESULT_WINDOW = 10000;
  public static final int MAXDISTANCE_LIMIT = 10000; // 10KM
  public static final int SERVICE_TIMEOUT = 3000;
}

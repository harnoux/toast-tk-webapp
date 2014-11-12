package controllers.mongo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import reactivemongo.api.MongoDriver
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.BSONDocumentReader
import scala.concurrent.Future
import reactivemongo.bson.BSONObjectID

object MongoConnector extends App {
  import reactivemongo.api._
  import scala.concurrent.ExecutionContext.Implicits.global
  lazy val driver = new MongoDriver
  val mongo_db_addr = "localhost";
  val db_name = "play_db";
  val config_collection_name = "configuration";
  
  def saveConfiguration(conf: Configuration) {
    val connection = driver.connection(List("localhost"))
    val db = connection("play_db")
    val collection = db("configuration")
    if(conf.id == null){
	    collection.insert(conf).onComplete {
	      case Failure(e) => throw e
	      case Success(_) => println("successfully inserted configuration !")
	    }
    }else{
    	collection.save(conf).onComplete {
	      case Failure(e) => throw e
	      case Success(_) => println("successfully saved configuration !")
	    }
    }
    
  }
  
  def saveAutoConfiguration(conf: AutoSetupConfig) {
    val connection = driver.connection(List("localhost"))
    val db = connection("play_db")
    val collection = db("repository")
    if(conf.id == null){
	    collection.insert(conf).onComplete {
	      case Failure(e) => throw e
	      case Success(_) => println("successfully inserted configuration !")
	    }
    }else{
    	collection.save(conf).onComplete {
	      case Failure(e) => throw e
	      case Success(_) => println("successfully saved configuration !")
	    }
    }
    
  }
  
  
  def loadConfiguration(): Future[List[Configuration]] = {
    val connection = driver.connection(List("localhost"))
    val db = connection("play_db")
    val collection = db("configuration")
    val query = BSONDocument()
    val configurations = collection.find(query).cursor[Configuration].collect[List]()
    configurations
  }
  
   def loadAutoConfiguration(): Future[List[AutoSetupConfig]] = {
    val connection = driver.connection(List("localhost"))
    val db = connection("play_db")
    val collection = db("repository")
    val query = BSONDocument()
    val configurations = collection.find(query).cursor[AutoSetupConfig].collect[List]()
    configurations
  }
  
  def loadWebPagesFromRepository(): Future[List[AutoSetupConfig]] = {
    val connection = driver.connection(List("localhost"))
    val db = connection("play_db")
    val collection = db("repository")
    val query = BSONDocument("type" -> "web page");
    val configurations = collection.find(query).cursor[AutoSetupConfig].collect[List]()
    configurations
    
  }
  
  def loadConfigurationSentences(confType:String, context:String): Future[List[Configuration]] = {
	val connection = driver.connection(List("localhost"))
    val db = connection("play_db")
    val collection = db("configuration")
    val query = BSONDocument("type" -> "service", "rows" -> BSONDocument("$elemMatch" -> BSONDocument("type" -> confType, "name" -> context)));
    val configurations = collection.find(query).cursor[Configuration].collect[List]()
    configurations	  
  }
}

class MongoConnector {

}


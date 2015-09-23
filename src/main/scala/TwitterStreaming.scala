import org.apache.spark.SparkConf
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}


object TwitterStreaming {



  def main(args: Array[String]) {


    val filters = args

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generate OAuth credentials

    System.setProperty("twitter4j.oauth.consumerKey", "tL2rd1P5ymgRmjX5BOLPD4vEy")
    System.setProperty("twitter4j.oauth.consumerSecret", "Eb4yxL5FQZEaU4pNPWvTxxiRVDIVxkciendJT2XvkvlB2tbdnv")
    System.setProperty("twitter4j.oauth.accessToken", "357650196-19WIIIoIu0bA7VRP0ZqkXRsdEJDQMbn9ckmZg1mL")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "6yWwJuNlu3gTVVG7ef0qdkjgFRMjWrtbz8T5pOq0RRt7g")

    //Create a spark configuration with a custom name and master
    // For more master configuration see  https://spark.apache.org/docs/1.2.0/submitting-applications.html#master-urls
    val sparkConf = new SparkConf().setAppName("STweetsApp").setMaster("local[*]")


    //Create a Streaming COntext with 2 second window
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    //Using the streaming context, open a twitter stream (By the way you can also use filters)
    //Stream generates a series of random tweets
    val stream = TwitterUtils.createStream(ssc, None, filters)
    stream.print()
    //Map : Retrieving Hash Tags
    val hashTags = stream.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))

    //Finding the top hash Tags on 30 second window
    val topCounts30 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(30))
      .map { case (topic, count) => (count, topic) }
      .transform(_.sortByKey(false))

    topCounts30.foreachRDD(rdd => {
      val topList = rdd.take(10)
      SocketClient.sendCommandToRobot( rdd.first() + "First in the Popular topics  in last 30 secs")
      println("\nPopular topics in last 30 seconds (%s total):".format(rdd.count()))
      topList.foreach { case (count, tag) => println("%s (%s tweets)".format(tag, count)) }
    })
    ssc.start()

    ssc.awaitTermination()
  }


}
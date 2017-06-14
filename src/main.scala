
import sun.misc.BASE64Decoder
import sun.misc.BASE64Encoder
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.OutputStream
import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage
import java.net.InetSocketAddress
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.NearbySearchRequest
import com.google.maps.model.PlaceType
import com.google.gson.{JsonObject, JsonParser}
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset


object HelloWorld {
  def ejer1(origen: String,destino: String): String ={
    val api_key = "AIzaSyAlXq_VP82-PR8eLsemhkRV2CUby8inPmE"
    val context = new GeoApiContext().setApiKey(api_key)
    val result = DirectionsApi.getDirections(context, origen, destino).await
    var x = 0
    var jsonString = "{\n\t\"ruta\": ["
    while ( {
      x < result.routes(0).legs(0).steps.size
    }) {
      val lat = result.routes(0).legs(0).steps(x).startLocation.lat
      val lng = result.routes(0).legs(0).steps(x).startLocation.lng
      jsonString += "\n\t\t{\n\t\t\"lat\": "+lat+",\n\t\t\"lng\": "+lng+"\n\t\t},"
      x += 1
    }
    jsonString = jsonString.substring(0, jsonString.length - 1)
    jsonString += "\n\t]\n}"
    return jsonString
  }

  def ejer2(origen: String): String ={
    val api_key = "AIzaSyAlXq_VP82-PR8eLsemhkRV2CUby8inPmE"
    val context = new GeoApiContext().setApiKey(api_key)
    val results = GeocodingApi.geocode(context, origen).await

    val request = new NearbySearchRequest(context)
    request.location(results(0).geometry.location)
    request.radius(500)
    request.`type`(PlaceType.RESTAURANT)
    val response = request.await

    var jsonString = "{\n\t\"restaurantes\": ["
    var x = 0
    while ( {
      x < response.results.size
    }) {
      val name = response.results(x).name
      val lat = response.results(x).geometry.location.lat
      val lng = response.results(x).geometry.location.lng
      jsonString += "\n\t\t{\n\t\t\t\"nombre\": \""+name+"\",\n\t\t\t\"lat\": "+lat+",\n\t\t\t\"lng\": "+lng+"\n\t\t},"
      x += 1
    }
    jsonString = jsonString.substring(0, jsonString.length - 1)
    jsonString += "\n\t]\n}"
    return jsonString
  }

  def ejer3(nombre: String,data: String): String ={
    val btDataFile = new BASE64Decoder().decodeBuffer(data)
    val of =  new File(nombre+".bmp")
    val osf = new FileOutputStream(of)
    osf.write(btDataFile)
    val imageRead = ImageIO.read(of)
    val w = imageRead.getWidth()
    val h = imageRead.getHeight()
    var x=0
    var y=0
    for (x <- 0 to w-1) {
      for (y <- 0 to h-1){
        var color = new Color(imageRead.getRGB(x,y))
        var red = (color.getRed*0.299).toInt
        var green = (color.getGreen*0.587).toInt
        var blue = (color.getBlue*0.114).toInt
        var gray = red + green + blue
        var newColor = new Color(gray,gray,gray)
        imageRead.setRGB(x,y,newColor.getRGB)
      }
    }
    val finalFile = new File("grayscale_"+nombre)
    ImageIO.write(imageRead,"bmp",finalFile)
    var imageStr:String = null
    var BOS = new ByteArrayOutputStream()
    ImageIO.write(imageRead, "bmp", BOS)
    val imageBytes = BOS.toByteArray()
    val encoder = new BASE64Encoder()
    imageStr= encoder.encode(imageBytes)
    BOS.close()
    val jsonString = "{\n\t\"nombre\": \"gris_"+nombre+"\",\n\t\"data\": \""+imageStr+"\"\n}"
    return jsonString
  }

  def resize(inputImagePath: String,
    outputImagePath: String, scaledWidth: Int, scaledHeight: Int) {
    val inputFile = new File(inputImagePath)
    val inputImage = ImageIO.read(inputFile)

    // creates output image
    val outputImage = new BufferedImage(scaledWidth,
      scaledHeight, inputImage.getType)

    // scales the input image to the output image
    val g2d = outputImage.createGraphics()
    g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null)
    g2d.dispose()

    // extracts extension of output file
    val formatName = outputImagePath.substring(outputImagePath
      .lastIndexOf(".") + 1)

    // writes to output file
    ImageIO.write(outputImage, formatName, new File(outputImagePath))
  }

  def ejer4(nombre: String,data: String,height: Int,wight: Int): String={
    var image: BufferedImage = null
    var imageByte: Array[Byte] = null

    val decoder = new BASE64Decoder()
    imageByte = decoder.decodeBuffer(data)
    val bis = new ByteArrayInputStream(imageByte)
    image = ImageIO.read(bis)
    bis.close()

    if (wight*height>image.getWidth*image.getHeight){
      return "{\n\t\"error\": \"Las dimensiones tienen que ser pequeñas\"\n}"
    }
    var outputfile = new File(nombre)
    ImageIO.write(image, "bmp", outputfile)
    resize(nombre,"resize_"+nombre,wight,height)

    outputfile = new File("resize_"+nombre)
    val imageRead = ImageIO.read(outputfile)
    val bos = new ByteArrayOutputStream()
    var imageString: String = null

    ImageIO.write (imageRead, "bmp", bos)
    val imageBytes = bos.toByteArray ()

    val encoder = new BASE64Encoder ()
    imageString = encoder.encode (imageBytes)

    bos.close ()
    val jsonString = "{\n\t\"nombre\": \"resize_"+nombre+"\",\n\t\"data\": \""+imageString+"\"\n}"
    return jsonString
  }
  //-------------Server----------------

  def main(args: Array[String]): Unit = {
    var server = HttpServer.create(new InetSocketAddress(8080),0)
    server.createContext("/ejercicio1", new ejercicio1())
    server.createContext("/ejercicio2", new ejercicio2())
    server.createContext("/ejercicio3", new ejercicio3())
    server.createContext("/ejercicio4", new ejercicio4())
    server.setExecutor(null)
    println("Listening Port 8080...")
    server.start()
  }

  class ejercicio1() extends HttpHandler{
    override def handle(t: HttpExchange){
      if (t.getRequestMethod=="POST"){
        val os: OutputStream = t.getResponseBody
        val isr = new InputStreamReader(t.getRequestBody, "utf-8")
        val br = new BufferedReader(isr)
        var thisLine: String = ""
        var myJson = ""
        while (thisLine !=null){
          myJson += thisLine
          thisLine = br.readLine()
        }
        val parser = new JsonParser
        var obj = parser.parse(myJson).getAsJsonObject
        var element = obj.get("origen")
        var origen = element.getAsString
        element = obj.get("destino")
        var destino = element.getAsString
        var response = ejer1(origen,destino).getBytes(Charset.forName("UTF-8"))
        t.getResponseHeaders().add("content-type","json")
        t.sendResponseHeaders(200,response.size.toLong)
        os.write(response)
        os.close()
      }
    }
  }

  class ejercicio2() extends HttpHandler{
    override def handle(t: HttpExchange){
      if (t.getRequestMethod=="POST"){
        val os: OutputStream = t.getResponseBody
        val isr = new InputStreamReader(t.getRequestBody, "utf-8")
        val br = new BufferedReader(isr)
        var thisLine: String = ""
        var myJson = ""
        while (thisLine !=null){
          myJson += thisLine
          thisLine = br.readLine()
        }
        val parser = new JsonParser
        var obj = parser.parse(myJson).getAsJsonObject
        var element = obj.get("origen")
        var origen = element.getAsString
        var response = ejer2(origen).getBytes(Charset.forName("UTF-8"))
        t.getResponseHeaders().add("content-type","json")
        t.sendResponseHeaders(200,response.size.toLong)
        os.write(response)
        os.close()
      }
    }
  }

  class ejercicio3() extends HttpHandler{
    override def handle(t: HttpExchange){
      if (t.getRequestMethod=="POST"){
        val os: OutputStream = t.getResponseBody
        val isr = new InputStreamReader(t.getRequestBody, "utf-8")
        val br = new BufferedReader(isr)
        var thisLine: String = ""
        var myJson = ""
        while (thisLine !=null){
          myJson += thisLine
          thisLine = br.readLine()
        }
        val parser = new JsonParser
        var obj = parser.parse(myJson).getAsJsonObject
        var element = obj.get("nombre")
        var nombre = element.getAsString
        element = obj.get("data")
        var data = element.getAsString
        var response = ejer3(nombre,data).getBytes(Charset.forName("UTF-8"))
        t.getResponseHeaders().add("content-type","json")
        t.sendResponseHeaders(200,response.size.toLong)
        os.write(response)
        os.close()
      }
    }
  }

  class ejercicio4() extends HttpHandler{
    override def handle(t: HttpExchange){
      if (t.getRequestMethod=="POST"){
        val os: OutputStream = t.getResponseBody
        val isr = new InputStreamReader(t.getRequestBody, "utf-8")
        val br = new BufferedReader(isr)
        var thisLine: String = ""
        var myJson = ""
        while (thisLine !=null){
          myJson += thisLine
          thisLine = br.readLine()
        }
        val parser = new JsonParser
        var obj = parser.parse(myJson).getAsJsonObject
        var element = obj.get("nombre")
        var nombre = element.getAsString
        element = obj.get("data")
        var data = element.getAsString
        obj = parser.parse(obj.get("tamaño").toString).getAsJsonObject
        var alto = obj.get("alto").getAsInt
        var ancho = obj.get("ancho").getAsInt
        var response = ejer4(nombre,data,alto,ancho).getBytes(Charset.forName("UTF-8"))
        t.getResponseHeaders().add("content-type","json")
        t.sendResponseHeaders(200,response.size.toLong)
        os.write(response)
        os.close()
        println(alto+" - "+ancho)
      }
    }
  }
}





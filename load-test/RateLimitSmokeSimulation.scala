import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class RateLimitSmokeSimulation extends Simulation {
  private val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  private val login = exec(
    http("login")
      .post("/api/auth/login")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"username":"demo","password":"password"}"""))
      .check(status.in(200, 429))
  )

  private val products = exec(
    http("products")
      .get("/ping")
      .check(status.is(200))
  )

  setUp(
    scenario("steady").exec(login).pause(100.millis).exec(products)
      .inject(constantUsersPerSec(20).during(30.seconds)),
    scenario("burst").exec(login)
      .inject(atOnceUsers(50))
  ).protocols(httpProtocol)
}


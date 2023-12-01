
import com.simiacryptus.skyenet.core.platform.AuthorizationInterface
import com.simiacryptus.skyenet.core.platform.User
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

open class AuthorizationInterfaceTest(
  val authInterface: AuthorizationInterface
) {

  open val user = User(email = "newuser@example.com", name = "Jane Smith", id = "2", picture = "http://example.com/newpicture.jpg")

  @Test
  fun `newUser has admin`() {
    assertTrue(authInterface.isAuthorized(this.javaClass, user, AuthorizationInterface.OperationType.Admin))
  }

}

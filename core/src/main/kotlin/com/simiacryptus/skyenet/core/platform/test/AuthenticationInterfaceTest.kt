
import com.simiacryptus.skyenet.core.platform.AuthenticationInterface
import com.simiacryptus.skyenet.core.platform.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

open class AuthenticationInterfaceTest(
  val authInterface: AuthenticationInterface
) {

  private val validAccessToken = UUID.randomUUID().toString()
  private val newUser = User(email = "newuser@example.com", name = "Jane Smith", id = "2", picture = "http://example.com/newpicture.jpg")

  @Test
  fun `getUser should return null when no user is associated with access token`() {
    val user = authInterface.getUser(validAccessToken)
    assertNull(user)
  }

  @Test
  fun `putUser should add a new user and return the user`() {
    val returnedUser = authInterface.putUser(validAccessToken, newUser)
    assertEquals(newUser, returnedUser)
  }

  @Test
  fun `getUser should return User after putUser is called`() {
    authInterface.putUser(validAccessToken, newUser)
    val user = authInterface.getUser(validAccessToken)
    assertNotNull(user)
    assertEquals(newUser, user)
  }

  @Test
  fun `containsUser should return false for non-existing user`() {
    val exists = authInterface.containsUser(newUser.email)
    assertFalse(exists)
  }

  @Test
  fun `containsUser should return true after user is added`() {
    authInterface.putUser(validAccessToken, newUser)
    val exists = authInterface.containsUser(newUser.email)
    assertTrue(exists)
  }

  @Test
  fun `logout should remove the user associated with the access token`() {
    authInterface.putUser(validAccessToken, newUser)
    assertNotNull(authInterface.getUser(validAccessToken))

    authInterface.logout(validAccessToken, newUser)
    assertNull(authInterface.getUser(validAccessToken))
  }

}

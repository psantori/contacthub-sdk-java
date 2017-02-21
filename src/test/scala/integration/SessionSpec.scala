package it.contactlab.hub.sdk.java.sync.test.integration;

import it.contactlab.hub.sdk.java.sync.ContactHub
import it.contactlab.hub.sdk.java.Auth
import org.scalatest.FeatureSpec
import org.scalatest.Matchers._
import org.scalatest.GivenWhenThen

class SessionSpec extends FeatureSpec with GivenWhenThen {

  val auth = new Auth(
    sys.env("CONTACTHUB_TEST_TOKEN"),
    sys.env("CONTACTHUB_TEST_WORKSPACE_ID"),
    sys.env("CONTACTHUB_TEST_NODE_ID")
  );

  val ch = new ContactHub(auth)
  val customerId = "4012e67d-72fd-4f84-9039-71cbc5b80078"

  feature("sessions") {
    scenario("generating a new sessionId", Integration) {
      Given("the user needs a new sessionId")
      When("the user calls createSessionId")
      val newId = ch.createSessionId()
      Then("the user gets a new UUIDv4")
      noException should be thrownBy java.util.UUID.fromString(newId)
    }

    scenario("reconciling a session", Integration) {
      Given("a customerId and a sessionId")
      val cid = customerId;
      val sid = java.util.UUID.randomUUID().toString();

      When("the user reconciles the session")
      val success = ch.addCustomerSession(cid, sid);

      Then("the session is added successfully")
      success should be (true)
    }
  }

}

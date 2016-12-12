package it.contactlab.hub.sdk.java.sync.test.integration;

import org.scalatest.FeatureSpec
import org.scalatest.Matchers._
import org.scalatest.GivenWhenThen

import it.contactlab.hub.sdk.java.sync.ContactHub
import it.contactlab.hub.sdk.java.Auth
import it.contactlab.hub.sdk.java.models._, base._
import it.contactlab.hub.sdk.java.exceptions._

import org.scalacheck.Gen

class CustomersSpec extends FeatureSpec with GivenWhenThen {

  implicit val genCustomer: Gen[Customer] = for {
    firstName <- Gen.alphaStr
    lastName  <- Gen.alphaStr
    email     <- Gen.alphaStr
  } yield {
    val contacts = new Contacts
    val base = new BaseProperties
    base.setFirstName(firstName)
    base.setLastName(lastName)
    contacts.setEmail(s"$email@example.com")
    base.setContacts(contacts)

    Customer.builder.base(base).build
  }

  val auth = new Auth(
    "97841617075b4b5f8ea88c30a8d2aec7647b7181df2c483fa78138c8d58aed4d",
    "40b6195f-e4f7-4f95-b10e-75268d850988",
    "854f0791-c120-4e4a-9264-6dd197cb922c"
  )

  val ch = new ContactHub(auth)
  val customerId = "c841ab14-b8a2-45d3-88b8-02210e2a9ebe"
  val externalId = "db55ec278cd6ca385c6d6a1ae49987c2"

  feature("retrieving customers") {
    scenario("retrieving the first page of customers of a node", Integration) {
      Given("a node")

      When("the user asks for the first page of customers")
      val customers = ch.getCustomers

      Then("all 10 users should be retrieved")
      customers shouldNot be (null)
      customers should have length 10
    }

    scenario("retrieving a single existing customer of a node by id", Integration) {
      Given("a node")

      When("the user asks for a customer by id")
      val customer = ch.getCustomer(customerId)

      Then("the user should be retrieved")
      customer.id.get shouldBe customerId
    }

    scenario("retrieving a single non-existing customer of a node by id", Integration) {
      Given("a node")
      When("the user asks for a user that doesn't exist")
      Then("the user should not be retrieved")
      Then("an error should be thrown")
      an [HttpException] should be thrownBy ch.getCustomer("not-existing")
    }

    scenario("retrieving a single customer of a node by external id", Integration) {
      Given("a node")

      When("the user asks for a customer by external id")
      val customer = ch.getCustomerByExternalId(externalId)

      Then("the user should be retrieved")
      customer.externalId.get shouldBe externalId
    }

    scenario("retrieving a single non-existing customer of a node by external id", Integration) {
      Given("a node")
      When("the user asks for a user that doesn't exist")
      Then("the user should not be retrieved")
      Then("an error should be thrown")

      an [HttpException] should be thrownBy ch.getCustomerByExternalId("not-existing")
    }

    scenario("reading all customer properties", Integration) {
      Given("a node")

      When("the user asks for a customer by id")
      val customer = ch.getCustomer(customerId)

      Then("the customer should have the expected id")
      customer.id.get shouldBe customerId

      And("the customer should have the expected tags")
      customer.tags.get.manual.get.toArray shouldBe Array("example-tag")
    }
  }

  feature("adding and deleting customers") {
    scenario("adding a customer to a node and deleting it", Integration) {
      Given("a new customer")
      val customer = genCustomer.sample.get

      When("the user adds a customer")
      val newCustomer = ch.addCustomer(customer);

      Then("a new customer is created")
      val id = newCustomer.id.get
      id shouldNot be (null)

      When("the user deletes the customer")
      val success = ch.deleteCustomer(id)

      Then("the customer should be deleted")
      success should be (true)
      an [HttpException] should be thrownBy ch.getCustomer(id)
    }

    scenario("deleting a non-exiting customer", Integration) {
      Given("a customer id")
      val customerId = "not-existing"
      Given("a node that doesn't contain a customer with such id")

      When("the user tries to delete that customer")

      Then("no customer deleted")
      an [HttpException] should be thrownBy ch.deleteCustomer(customerId)
    }
  }

  feature("updating and patching customers") {
    scenario("updating a customer's email address") {
      Given("a customer previously added")
      val customer = genCustomer.sample.get
      val newCustomer = ch.addCustomer(customer)

      When("the user updates the customer")
      val base = newCustomer.base.get
      val contacts = base.getContacts
      val newEmail = Gen.alphaStr.sample.get + "@example.com"
      contacts.setEmail(newEmail)
      base.setContacts(contacts)

      val updatedCustomer = ch.updateCustomer(
        Customer.builder
          .id(newCustomer.id)
          .base(base)
          .build
      )

      Then("the customer should be updated")
      ch.getCustomer(newCustomer.id.get) shouldBe updatedCustomer

      Then("the customer's id should not have changed")
      updatedCustomer.id.get shouldBe newCustomer.id.get

      Then("the customer's email should be updated")
      updatedCustomer.base.get.getContacts.getEmail shouldBe newEmail

      Then("the customer's updatedAt date should be updated")
      updatedCustomer.updatedAt.get should be > newCustomer.updatedAt.get
    }

    scenario("updating a non-existing customer") {
      Given("a node")
      When("the user tries to update a user that does not exist")
      val customer = Customer.builder.id("not-existing").build

      Then("the update should fail")
      an [HttpException] should be thrownBy ch.updateCustomer(customer)
    }

    scenario("patching a customer's email address") {
      Given("a customer previously added")
      val customer = genCustomer.sample.get
      val newCustomer = ch.addCustomer(customer)

      When("the user patches the customer")
      val contacts = new Contacts
      val base = new BaseProperties
      val newEmail = Gen.alphaStr.sample.get + "@example.com"
      contacts.setEmail(newEmail)
      base.setContacts(contacts)
      val patchCustomer = Customer.builder.base(base).build;
      val updatedCustomer = ch.patchCustomer(newCustomer.id.get, patchCustomer)

      Then("the customer should be updated")
      ch.getCustomer(newCustomer.id.get) shouldBe updatedCustomer

      Then("the customer's id should not have changed")
      updatedCustomer.id shouldBe newCustomer.id

      Then("the customer's first and last name should not have changed")
      updatedCustomer.base.get.getFirstName shouldBe newCustomer.base.get.getFirstName
      updatedCustomer.base.get.getLastName shouldBe newCustomer.base.get.getLastName

      Then("the customer's email should be updated")
      updatedCustomer.base.get.getContacts.getEmail shouldBe newEmail
      Then("the customer's updatedAt date should be updated")
      updatedCustomer.updatedAt.get should be > newCustomer.updatedAt.get
    }

    scenario("patching a non-existing customer") {
      Given("some customer data")
      val newCustomer = genCustomer.sample.get
      When("the user tries to update a user that does not exist")
      def patch = ch.patchCustomer("non-existing", newCustomer)
      Then("the patch should fail")
      an [HttpException] should be thrownBy patch
    }
  }

}

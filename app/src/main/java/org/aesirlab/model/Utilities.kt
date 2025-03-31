package org.aesirlab.model

import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.rdf.model.ResourceFactory
import kotlin.String

public class Utilities {
  public companion object {
    public const val ABSOLUTE_URI: String = "TEST_LISTOF_itemThing"

    public const val NS_ACP: String = "http://www.w3.org/ns/solid/acp#"

    public const val NS_ACL: String = "http://www.w3.org/ns/auth/acl#"

    public const val NS_LDP: String = "http://www.w3.org/ns/ldp#"

    public const val NS_SKOS: String = "http://www.w3.org/2004/02/skos/core#"

    public const val NS_SOLID: String = "http://www.w3.org/ns/solid/terms#"

    public const val NS_Item: String = "https://example.org/item#"

    public fun resourceToItem(resource: Resource): Item {
      val anonModel = ModelFactory.createDefaultModel()
      val id = resource.uri.split("#")[1]
      val nameProp = anonModel.createProperty(NS_Item + "name")
      val nameObject = resource.getProperty(nameProp).`object`
      val nameLiteral = ResourceFactory.createTypedLiteral(nameObject)
      val name = nameLiteral.value.toString().split("^^")[0]
      val amountProp = anonModel.createProperty(NS_Item + "amount")
      val amountObject = resource.getProperty(amountProp).`object`
      val amountLiteral = ResourceFactory.createTypedLiteral(amountObject)
      val amount = amountLiteral.value.toString().split("^^")[0].toInt()
      return Item(id, name, amount)
    }
  }
}

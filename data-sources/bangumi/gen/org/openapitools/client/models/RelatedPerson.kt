/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package org.openapitools.client.models

import org.openapitools.client.models.PersonCareer
import org.openapitools.client.models.PersonImages
import org.openapitools.client.models.PersonType

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param id 
 * @param name 
 * @param type 
 * @param career 
 * @param relation 
 * @param images 
 */


data class RelatedPerson (

    @Json(name = "id")
    val id: kotlin.Int,

    @Json(name = "name")
    val name: kotlin.String,

    @Json(name = "type")
    val type: PersonType,

    @Json(name = "career")
    val career: kotlin.collections.List<PersonCareer>,

    @Json(name = "relation")
    val relation: kotlin.String,

    @Json(name = "images")
    val images: PersonImages? = null

)

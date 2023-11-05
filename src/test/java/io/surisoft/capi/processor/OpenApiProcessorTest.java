package io.surisoft.capi.processor;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenApiProcessorTest {

    private OpenAPI mockOpenAPI;

    private OpenApiProcessor openApiProcessorUnderTest;

    private String openApiDefinition = "openapi: 3.0.0\n" +
            "info:\n" +
            "  title: OpenAPI Petstore\n" +
            "  description: This is a sample server Petstore server. For this sample, you can use the api key `special-key` to test the authorization filters. For OAuth2 flow, you may use `user` as both username and password when asked to login.\n" +
            "  license:\n" +
            "    name: Apache-2.0\n" +
            "    url: http://www.apache.org/licenses/LICENSE-2.0.html\n" +
            "  version: 1.0.0\n" +
            "externalDocs:\n" +
            "  description: Find out more about OpenAPI generator\n" +
            "  url: https://openapi-generator.tech\n" +
            "tags:\n" +
            "- name: pet\n" +
            "  description: Everything about your Pets\n" +
            "- name: store\n" +
            "  description: Access to Petstore orders\n" +
            "- name: user\n" +
            "  description: Operations about user\n" +
            "paths:\n" +
            "  /pet:\n" +
            "    put:\n" +
            "      tags:\n" +
            "      - pet\n" +
            "      summary: Update an existing pet\n" +
            "      operationId: updatePet\n" +
            "      requestBody:\n" +
            "        $ref: '#/components/requestBodies/Pet'\n" +
            "      responses:\n" +
            "        400:\n" +
            "          description: Invalid ID supplied\n" +
            "        404:\n" +
            "          description: Pet not found\n" +
            "        405:\n" +
            "          description: Validation exception\n" +
            "      security:\n" +
            "      - petstore_auth:\n" +
            "        - write:pets\n" +
            "        - read:pets\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: pet\n" +
            "      x-contentType: application/json\n" +
            "    post:\n" +
            "      tags:\n" +
            "      - pet\n" +
            "      summary: Add a new pet to the store\n" +
            "      operationId: addPet\n" +
            "      requestBody:\n" +
            "        $ref: '#/components/requestBodies/Pet'\n" +
            "      responses:\n" +
            "        405:\n" +
            "          description: Invalid input\n" +
            "      security:\n" +
            "      - petstore_auth:\n" +
            "        - write:pets\n" +
            "        - read:pets\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: pet\n" +
            "      x-contentType: application/json\n" +
            "  /pet/findByStatus:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - pet\n" +
            "      summary: Finds Pets by status\n" +
            "      description: Multiple status values can be provided with comma separated strings\n" +
            "      operationId: findPetsByStatus\n" +
            "      parameters:\n" +
            "      - name: status\n" +
            "        in: query\n" +
            "        description: Status values that need to be considered for filter\n" +
            "        required: true\n" +
            "        style: form\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: array\n" +
            "          items:\n" +
            "            type: string\n" +
            "            default: available\n" +
            "            enum:\n" +
            "            - available\n" +
            "            - pending\n" +
            "            - sold\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: successful operation\n" +
            "          content:\n" +
            "            application/xml:\n" +
            "              schema:\n" +
            "                type: array\n" +
            "                items:\n" +
            "                  $ref: '#/components/schemas/Pet'\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                type: array\n" +
            "                items:\n" +
            "                  $ref: '#/components/schemas/Pet'\n" +
            "        400:\n" +
            "          description: Invalid status value\n" +
            "      security:\n" +
            "      - petstore_auth:\n" +
            "        - write:pets\n" +
            "        - read:pets\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: pet\n" +
            "  /pet/findByTags:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - pet\n" +
            "      summary: Finds Pets by tags\n" +
            "      description: Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.\n" +
            "      operationId: findPetsByTags\n" +
            "      parameters:\n" +
            "      - name: tags\n" +
            "        in: query\n" +
            "        description: Tags to filter by\n" +
            "        required: true\n" +
            "        style: form\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: array\n" +
            "          items:\n" +
            "            type: string\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: successful operation\n" +
            "          content:\n" +
            "            application/xml:\n" +
            "              schema:\n" +
            "                type: array\n" +
            "                items:\n" +
            "                  $ref: '#/components/schemas/Pet'\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                type: array\n" +
            "                items:\n" +
            "                  $ref: '#/components/schemas/Pet'\n" +
            "        400:\n" +
            "          description: Invalid tag value\n" +
            "      deprecated: true\n" +
            "      security:\n" +
            "      - petstore_auth:\n" +
            "        - write:pets\n" +
            "        - read:pets\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: pet\n" +
            "  /pet/{petId}:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - pet\n" +
            "      summary: Find pet by ID\n" +
            "      description: Returns a single pet\n" +
            "      operationId: getPetById\n" +
            "      parameters:\n" +
            "      - name: petId\n" +
            "        in: path\n" +
            "        description: ID of pet to return\n" +
            "        required: true\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: successful operation\n" +
            "          content:\n" +
            "            application/xml:\n" +
            "              schema:\n" +
            "                $ref: '#/components/schemas/Pet'\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: '#/components/schemas/Pet'\n" +
            "        400:\n" +
            "          description: Invalid ID supplied\n" +
            "        404:\n" +
            "          description: Pet not found\n" +
            "      security:\n" +
            "      - api_key: []\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: pet\n" +
            "    post:\n" +
            "      tags:\n" +
            "      - pet\n" +
            "      summary: Updates a pet in the store with form data\n" +
            "      operationId: updatePetWithForm\n" +
            "      parameters:\n" +
            "      - name: petId\n" +
            "        in: path\n" +
            "        description: ID of pet that needs to be updated\n" +
            "        required: true\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "      requestBody:\n" +
            "        content:\n" +
            "          application/x-www-form-urlencoded:\n" +
            "            schema:\n" +
            "              $ref: '#/components/schemas/body'\n" +
            "      responses:\n" +
            "        405:\n" +
            "          description: Invalid input\n" +
            "      security:\n" +
            "      - petstore_auth:\n" +
            "        - write:pets\n" +
            "        - read:pets\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: pet\n" +
            "      x-contentType: application/x-www-form-urlencoded\n" +
            "    delete:\n" +
            "      tags:\n" +
            "      - pet\n" +
            "      summary: Deletes a pet\n" +
            "      operationId: deletePet\n" +
            "      parameters:\n" +
            "      - name: api_key\n" +
            "        in: header\n" +
            "        required: false\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: string\n" +
            "      - name: petId\n" +
            "        in: path\n" +
            "        description: Pet id to delete\n" +
            "        required: true\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "      responses:\n" +
            "        400:\n" +
            "          description: Invalid pet value\n" +
            "      security:\n" +
            "      - petstore_auth:\n" +
            "        - write:pets\n" +
            "        - read:pets\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: pet\n" +
            "  /pet/{petId}/uploadImage:\n" +
            "    post:\n" +
            "      tags:\n" +
            "      - pet\n" +
            "      summary: uploads an image\n" +
            "      operationId: uploadFile\n" +
            "      parameters:\n" +
            "      - name: petId\n" +
            "        in: path\n" +
            "        description: ID of pet to update\n" +
            "        required: true\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "      requestBody:\n" +
            "        content:\n" +
            "          multipart/form-data:\n" +
            "            schema:\n" +
            "              $ref: '#/components/schemas/body_1'\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: successful operation\n" +
            "          content:\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: '#/components/schemas/ApiResponse'\n" +
            "      security:\n" +
            "      - petstore_auth:\n" +
            "        - write:pets\n" +
            "        - read:pets\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: pet\n" +
            "      x-contentType: multipart/form-data\n" +
            "  /store/inventory:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - store\n" +
            "      summary: Returns pet inventories by status\n" +
            "      description: Returns a map of status codes to quantities\n" +
            "      operationId: getInventory\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: successful operation\n" +
            "          content:\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                type: object\n" +
            "                additionalProperties:\n" +
            "                  type: integer\n" +
            "                  format: int32\n" +
            "      security:\n" +
            "      - api_key: []\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: store\n" +
            "  /store/order:\n" +
            "    post:\n" +
            "      tags:\n" +
            "      - store\n" +
            "      summary: Place an order for a pet\n" +
            "      operationId: placeOrder\n" +
            "      requestBody:\n" +
            "        description: order placed for purchasing the pet\n" +
            "        content:\n" +
            "          application/json:\n" +
            "            schema:\n" +
            "              $ref: '#/components/schemas/Order'\n" +
            "        required: true\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: successful operation\n" +
            "          content:\n" +
            "            application/xml:\n" +
            "              schema:\n" +
            "                $ref: '#/components/schemas/Order'\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: '#/components/schemas/Order'\n" +
            "        400:\n" +
            "          description: Invalid Order\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: store\n" +
            "      x-contentType: application/json\n" +
            "  /store/order/{orderId}:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - store\n" +
            "      summary: Find purchase order by ID\n" +
            "      description: For valid response try integer IDs with value <= 5 or > 10. Other values will generated exceptions\n" +
            "      operationId: getOrderById\n" +
            "      parameters:\n" +
            "      - name: orderId\n" +
            "        in: path\n" +
            "        description: ID of pet that needs to be fetched\n" +
            "        required: true\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          maximum: 5\n" +
            "          minimum: 1\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: successful operation\n" +
            "          content:\n" +
            "            application/xml:\n" +
            "              schema:\n" +
            "                $ref: '#/components/schemas/Order'\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: '#/components/schemas/Order'\n" +
            "        400:\n" +
            "          description: Invalid ID supplied\n" +
            "        404:\n" +
            "          description: Order not found\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: store\n" +
            "    delete:\n" +
            "      tags:\n" +
            "      - store\n" +
            "      summary: Delete purchase order by ID\n" +
            "      description: For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors\n" +
            "      operationId: deleteOrder\n" +
            "      parameters:\n" +
            "      - name: orderId\n" +
            "        in: path\n" +
            "        description: ID of the order that needs to be deleted\n" +
            "        required: true\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: string\n" +
            "      responses:\n" +
            "        400:\n" +
            "          description: Invalid ID supplied\n" +
            "        404:\n" +
            "          description: Order not found\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: store\n" +
            "  /user:\n" +
            "    post:\n" +
            "      tags:\n" +
            "      - user\n" +
            "      summary: Create user\n" +
            "      description: This can only be done by the logged in user.\n" +
            "      operationId: createUser\n" +
            "      requestBody:\n" +
            "        description: Created user object\n" +
            "        content:\n" +
            "          application/json:\n" +
            "            schema:\n" +
            "              $ref: '#/components/schemas/User'\n" +
            "        required: true\n" +
            "      responses:\n" +
            "        default:\n" +
            "          description: successful operation\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: user\n" +
            "      x-contentType: application/json\n" +
            "  /user/createWithArray:\n" +
            "    post:\n" +
            "      tags:\n" +
            "      - user\n" +
            "      summary: Creates list of users with given input array\n" +
            "      operationId: createUsersWithArrayInput\n" +
            "      requestBody:\n" +
            "        $ref: '#/components/requestBodies/UserArray'\n" +
            "      responses:\n" +
            "        default:\n" +
            "          description: successful operation\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: user\n" +
            "      x-contentType: application/json\n" +
            "  /user/createWithList:\n" +
            "    post:\n" +
            "      tags:\n" +
            "      - user\n" +
            "      summary: Creates list of users with given input array\n" +
            "      operationId: createUsersWithListInput\n" +
            "      requestBody:\n" +
            "        $ref: '#/components/requestBodies/UserArray'\n" +
            "      responses:\n" +
            "        default:\n" +
            "          description: successful operation\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: user\n" +
            "      x-contentType: application/json\n" +
            "  /user/login:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - user\n" +
            "      summary: Logs user into the system\n" +
            "      operationId: loginUser\n" +
            "      parameters:\n" +
            "      - name: username\n" +
            "        in: query\n" +
            "        description: The user name for login\n" +
            "        required: true\n" +
            "        style: form\n" +
            "        explode: true\n" +
            "        schema:\n" +
            "          type: string\n" +
            "      - name: password\n" +
            "        in: query\n" +
            "        description: The password for login in clear text\n" +
            "        required: true\n" +
            "        style: form\n" +
            "        explode: true\n" +
            "        schema:\n" +
            "          type: string\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: successful operation\n" +
            "          headers:\n" +
            "            X-Rate-Limit:\n" +
            "              description: calls per hour allowed by the user\n" +
            "              style: simple\n" +
            "              explode: false\n" +
            "              schema:\n" +
            "                type: integer\n" +
            "                format: int32\n" +
            "            X-Expires-After:\n" +
            "              description: date in UTC when toekn expires\n" +
            "              style: simple\n" +
            "              explode: false\n" +
            "              schema:\n" +
            "                type: string\n" +
            "                format: date-time\n" +
            "          content:\n" +
            "            application/xml:\n" +
            "              schema:\n" +
            "                type: string\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                type: string\n" +
            "        400:\n" +
            "          description: Invalid username/password supplied\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: user\n" +
            "  /user/logout:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - user\n" +
            "      summary: Logs out current logged in user session\n" +
            "      operationId: logoutUser\n" +
            "      responses:\n" +
            "        default:\n" +
            "          description: successful operation\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: user\n" +
            "  /user/{username}:\n" +
            "    get:\n" +
            "      tags:\n" +
            "      - user\n" +
            "      summary: Get user by user name\n" +
            "      operationId: getUserByName\n" +
            "      parameters:\n" +
            "      - name: username\n" +
            "        in: path\n" +
            "        description: The name that needs to be fetched. Use user1 for testing.\n" +
            "        required: true\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: string\n" +
            "      responses:\n" +
            "        200:\n" +
            "          description: successful operation\n" +
            "          content:\n" +
            "            application/xml:\n" +
            "              schema:\n" +
            "                $ref: '#/components/schemas/User'\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: '#/components/schemas/User'\n" +
            "        400:\n" +
            "          description: Invalid username supplied\n" +
            "        404:\n" +
            "          description: User not found\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: user\n" +
            "    put:\n" +
            "      tags:\n" +
            "      - user\n" +
            "      summary: Updated user\n" +
            "      description: This can only be done by the logged in user.\n" +
            "      operationId: updateUser\n" +
            "      parameters:\n" +
            "      - name: username\n" +
            "        in: path\n" +
            "        description: name that need to be deleted\n" +
            "        required: true\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: string\n" +
            "      requestBody:\n" +
            "        description: Updated user object\n" +
            "        content:\n" +
            "          application/json:\n" +
            "            schema:\n" +
            "              $ref: '#/components/schemas/User'\n" +
            "        required: true\n" +
            "      responses:\n" +
            "        400:\n" +
            "          description: Invalid user supplied\n" +
            "        404:\n" +
            "          description: User not found\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: user\n" +
            "      x-contentType: application/json\n" +
            "    delete:\n" +
            "      tags:\n" +
            "      - user\n" +
            "      summary: Delete user\n" +
            "      description: This can only be done by the logged in user.\n" +
            "      operationId: deleteUser\n" +
            "      parameters:\n" +
            "      - name: username\n" +
            "        in: path\n" +
            "        description: The name that needs to be deleted\n" +
            "        required: true\n" +
            "        style: simple\n" +
            "        explode: false\n" +
            "        schema:\n" +
            "          type: string\n" +
            "      responses:\n" +
            "        400:\n" +
            "          description: Invalid username supplied\n" +
            "        404:\n" +
            "          description: User not found\n" +
            "      x-accepts: application/json\n" +
            "      x-tags:\n" +
            "      - tag: user\n" +
            "components:\n" +
            "  schemas:\n" +
            "    Order:\n" +
            "      title: Pet Order\n" +
            "      type: object\n" +
            "      properties:\n" +
            "        id:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "        petId:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "        quantity:\n" +
            "          type: integer\n" +
            "          format: int32\n" +
            "        shipDate:\n" +
            "          type: string\n" +
            "          format: date-time\n" +
            "        status:\n" +
            "          type: string\n" +
            "          description: Order Status\n" +
            "          enum:\n" +
            "          - placed\n" +
            "          - approved\n" +
            "          - delivered\n" +
            "        complete:\n" +
            "          type: boolean\n" +
            "          default: false\n" +
            "      description: An order for a pets from the pet store\n" +
            "      example:\n" +
            "        petId: 6\n" +
            "        quantity: 1\n" +
            "        id: 0\n" +
            "        shipDate: 2000-01-23T04:56:07.000+00:00\n" +
            "        complete: false\n" +
            "        status: placed\n" +
            "      xml:\n" +
            "        name: Order\n" +
            "    Category:\n" +
            "      title: Pet category\n" +
            "      type: object\n" +
            "      properties:\n" +
            "        id:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "        name:\n" +
            "          type: string\n" +
            "      description: A category for a pet\n" +
            "      example:\n" +
            "        name: name\n" +
            "        id: 6\n" +
            "      xml:\n" +
            "        name: Category\n" +
            "    User:\n" +
            "      title: a User\n" +
            "      type: object\n" +
            "      properties:\n" +
            "        id:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "        username:\n" +
            "          type: string\n" +
            "        firstName:\n" +
            "          type: string\n" +
            "        lastName:\n" +
            "          type: string\n" +
            "        email:\n" +
            "          type: string\n" +
            "        password:\n" +
            "          type: string\n" +
            "        phone:\n" +
            "          type: string\n" +
            "        userStatus:\n" +
            "          type: integer\n" +
            "          description: User Status\n" +
            "          format: int32\n" +
            "      description: A User who is purchasing from the pet store\n" +
            "      example:\n" +
            "        firstName: firstName\n" +
            "        lastName: lastName\n" +
            "        password: password\n" +
            "        userStatus: 6\n" +
            "        phone: phone\n" +
            "        id: 0\n" +
            "        email: email\n" +
            "        username: username\n" +
            "      xml:\n" +
            "        name: User\n" +
            "    Tag:\n" +
            "      title: Pet Tag\n" +
            "      type: object\n" +
            "      properties:\n" +
            "        id:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "        name:\n" +
            "          type: string\n" +
            "      description: A tag for a pet\n" +
            "      example:\n" +
            "        name: name\n" +
            "        id: 1\n" +
            "      xml:\n" +
            "        name: Tag\n" +
            "    Pet:\n" +
            "      title: a Pet\n" +
            "      required:\n" +
            "      - name\n" +
            "      - photoUrls\n" +
            "      type: object\n" +
            "      properties:\n" +
            "        id:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "        category:\n" +
            "          $ref: '#/components/schemas/Category'\n" +
            "        name:\n" +
            "          type: string\n" +
            "          example: doggie\n" +
            "        photoUrls:\n" +
            "          type: array\n" +
            "          xml:\n" +
            "            name: photoUrl\n" +
            "            wrapped: true\n" +
            "          items:\n" +
            "            type: string\n" +
            "        tags:\n" +
            "          type: array\n" +
            "          xml:\n" +
            "            name: tag\n" +
            "            wrapped: true\n" +
            "          items:\n" +
            "            $ref: '#/components/schemas/Tag'\n" +
            "        status:\n" +
            "          type: string\n" +
            "          description: pet status in the store\n" +
            "          enum:\n" +
            "          - available\n" +
            "          - pending\n" +
            "          - sold\n" +
            "      description: A pet for sale in the pet store\n" +
            "      example:\n" +
            "        photoUrls:\n" +
            "        - photoUrls\n" +
            "        - photoUrls\n" +
            "        name: doggie\n" +
            "        id: 0\n" +
            "        category:\n" +
            "          name: name\n" +
            "          id: 6\n" +
            "        tags:\n" +
            "        - name: name\n" +
            "          id: 1\n" +
            "        - name: name\n" +
            "          id: 1\n" +
            "        status: available\n" +
            "      xml:\n" +
            "        name: Pet\n" +
            "    ApiResponse:\n" +
            "      title: An uploaded response\n" +
            "      type: object\n" +
            "      properties:\n" +
            "        code:\n" +
            "          type: integer\n" +
            "          format: int32\n" +
            "        type:\n" +
            "          type: string\n" +
            "        message:\n" +
            "          type: string\n" +
            "      description: Describes the result of uploading an image resource\n" +
            "      example:\n" +
            "        code: 0\n" +
            "        type: type\n" +
            "        message: message\n" +
            "    body:\n" +
            "      type: object\n" +
            "      properties:\n" +
            "        name:\n" +
            "          type: string\n" +
            "          description: Updated name of the pet\n" +
            "        status:\n" +
            "          type: string\n" +
            "          description: Updated status of the pet\n" +
            "    body_1:\n" +
            "      type: object\n" +
            "      properties:\n" +
            "        additionalMetadata:\n" +
            "          type: string\n" +
            "          description: Additional data to pass to server\n" +
            "        file:\n" +
            "          type: string\n" +
            "          description: file to upload\n" +
            "          format: binary\n" +
            "  requestBodies:\n" +
            "    UserArray:\n" +
            "      description: List of user object\n" +
            "      content:\n" +
            "        application/json:\n" +
            "          schema:\n" +
            "            type: array\n" +
            "            items:\n" +
            "              $ref: '#/components/schemas/User'\n" +
            "      required: true\n" +
            "    Pet:\n" +
            "      description: Pet object that needs to be added to the store\n" +
            "      content:\n" +
            "        application/json:\n" +
            "          schema:\n" +
            "            $ref: '#/components/schemas/Pet'\n" +
            "        application/xml:\n" +
            "          schema:\n" +
            "            $ref: '#/components/schemas/Pet'\n" +
            "      required: true\n" +
            "  securitySchemes:\n" +
            "    petstore_auth:\n" +
            "      type: oauth2\n" +
            "      flows:\n" +
            "        implicit:\n" +
            "          authorizationUrl: /api/oauth/dialog\n" +
            "          scopes:\n" +
            "            write:pets: modify pets in your account\n" +
            "            read:pets: read your pets\n" +
            "    api_key:\n" +
            "      type: apiKey\n" +
            "      name: api_key\n" +
            "      in: header";

    @BeforeEach
    void setUp() {

    }

    @Test
    void testProcessWithAValidPathWithoutPlaceholders() throws Exception {
        // Setup
        mockOpenAPI = new OpenAPIV3Parser().readContents(openApiDefinition).getOpenAPI();
        openApiProcessorUnderTest = new OpenApiProcessor(mockOpenAPI);

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.getIn().setHeader("CamelHttpPath", "/pet/findByTags");
        exchange.getIn().setHeader("CamelHttpMethod", "GET");


        // Run the test
        openApiProcessorUnderTest.process(exchange);

        Assertions.assertNull(exchange.getException());
    }

    @Test
    void testProcessWithAValidPathWithPlaceholders() throws Exception {
        // Setup
        mockOpenAPI = new OpenAPIV3Parser().readContents(openApiDefinition).getOpenAPI();
        openApiProcessorUnderTest = new OpenApiProcessor(mockOpenAPI);

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.getIn().setHeader("CamelHttpPath", "/pet/123");
        exchange.getIn().setHeader("CamelHttpMethod", "GET");


        // Run the test
        openApiProcessorUnderTest.process(exchange);

        Assertions.assertNull(exchange.getException());
    }

    @Test
    void testProcessWithAnInvalidPath() throws Exception {
        // Setup
        mockOpenAPI = new OpenAPIV3Parser().readContents(openApiDefinition).getOpenAPI();
        openApiProcessorUnderTest = new OpenApiProcessor(mockOpenAPI);

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.getIn().setHeader("CamelHttpPath", "/pet/123");
        exchange.getIn().setHeader("CamelHttpMethod", "PUT");


        // Run the test
        openApiProcessorUnderTest.process(exchange);

        Assertions.assertNotNull(exchange.getException());
    }
}

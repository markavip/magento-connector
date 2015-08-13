/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.md file.
 */

package org.mule.module.magento.api.catalog;

import com.magento.api.*;

import org.mule.module.magento.api.catalog.model.MediaMimeType;
import org.mule.module.magento.api.catalog.model.ProductIdentifier;

import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

public interface MagentoCatalogClient<ExceptionType extends Exception>
{

    /**
     * Lists product of a given category. See catalog-category-assignedProducts SOAP
     * method.
     * 
     *
     * @param categoryId
     * @return the listing of category products
     */
    List<CatalogAssignedProduct> listCategoryProducts(int categoryId) throws ExceptionType;
    
    /**
     * Allows you to get full information about a required attribute with the list of options. See catalogProductAttributeInfo SOAP method.   
     *  
     *
     * @param attribute
     * @return CatalogProductAttributeEntity
     */
    public CatalogProductAttributeEntity getCatalogProductAttributeInfo(String attribute) throws ExceptionType;

    /**
     * Assign product to category. See catalog-category-assignProduct SOAP method
     *
     * @param categoryId
     * @param productId
     * @param position
     */
    boolean addCategoryProduct(int categoryId, @NotNull ProductIdentifier productId, String position)
        throws ExceptionType;

    /**
     * Creates a new category. See catalog-category-create SOAP method.
     * 
     * @param parentId
     * @param attributes
     * @param storeView
     * @return the new category id
     */
    int createCategory(int parentId, @NotNull CatalogCategoryEntityCreate attributes, String storeView)
        throws ExceptionType;

    /**
     * Answers the current default catalog store view id for this session
     * 
     * @return the current default store view id
     */
    int getCatalogCurrentStoreView() throws ExceptionType;

    /**
     * Set the default catalog store view for this session
     *
     * @param storeViewIdOrCode the id or code of the store view to set as default
     *            for this session
     */
    int updateCatalogCurrentStoreView(@NotNull String storeViewIdOrCode) throws ExceptionType;

    /**
     * Deletes a category. See catalog-category-delete SOAP method
     * 
     * @param categoryId
     * @return if the category is deleted
     */
    boolean deleteCategory(int categoryId) throws ExceptionType;

    /**
     * Answers category attributes. See catalog-category-info SOAP method.
     * 
     *
     * @param categoryId
     * @param storeView
     * @param attributeNames
     * @return the category attributes
     */
    CatalogCategoryInfo getCategory(int categoryId, String storeView, List<String> attributeNames)
        throws ExceptionType;

    /**
     * Answers levels of categories for a website, store view and parente category
     * 
     *
     * @param website
     * @param storeView
     * @param parentCategory
     * @return
     * @throws ExceptionType
     */
    List<CatalogCategoryEntityNoChildren> listCategoryLevels(String website, String storeView, String parentCategory)
        throws ExceptionType;

    /**
     * Move category in tree. See catalog-category-move SOAP method. NOTE Please make
     * sure that you are not moving category to any of its own children. There are no
     * extra checks to prevent doing it through webservices API, and you won’t be
     * able to fix this from admin interface then
     * 
     * @param categoryId
     * @param parentId
     * @param afterId
     */
    void moveCategory(int categoryId, int parentId, String afterId) throws ExceptionType;

    /**
     * Remove a product assignment. See catalog-category-removeProduct SOAP method.
     * 
     * @param categoryId
     * @param productId
     * @return if the product is removed from the category
     */
    boolean deleteCategoryProduct(int categoryId, @NotNull ProductIdentifier productId) throws ExceptionType;

    /**
     * Retrieve hierarchical tree. See catalog-category-tree SOAP method.
     * 
     *
     * @param parentId
     * @param storeView
     * @return
     */
    CatalogCategoryTree getCategoryTree(String parentId, String storeView) throws ExceptionType;

    /**
     * Updates a category. See catalog-category-update SOAP method
     *
     * @param categoryId
     * @param attributes
     * @param storeView
     */
    boolean updateCategory(int categoryId, @NotNull CatalogCategoryEntityCreate attributes, String storeView)
        throws ExceptionType;

    /**
     * @param categoryId
     * @param productId
     * @param position
     */
    boolean updateCategoryProduct(int categoryId, @NotNull ProductIdentifier productId, String position)
        throws ExceptionType;

    /**
     *
     * @param products
     * @return
     */
    List<CatalogInventoryStockItemEntity> listInventoryStockItems(@NotNull List<String> products) throws ExceptionType;

    /**
     * Updates an stock inventory item
     *
     * @param productId
     * @param attributes
     */
    int updateInventoryStockItem(@NotNull ProductIdentifier productId,
                                 @NotNull CatalogInventoryStockItemUpdateEntity attributes) throws ExceptionType;

    /**
     * Creates a new product
     * 
     * @param type the new product's type
     * @param set the new product's set
     * @param sku the new product's sku
     * @param attributes the standard attributes of the new product
     * @param additionalAttributes the non standard attributes of the new product
     * @return the new product's id
     */
    int createProduct(@NotNull String type,
                      @NotNull int set,
                      @NotNull String sku,
                      CatalogProductCreateEntity attributes,
                      List<AssociativeEntity> additionalAttributes,
                      String storeView) throws ExceptionType;

    /**
     * Deletes a product. See catalog-product-delete SOAP method.
     * 
     * @param productId the product identifier
     * @return if the product is deleted
     */
    int deleteProduct(ProductIdentifier productId) throws ExceptionType;

    /**
     * Answers a product special price. See catalog-product-getSpecialPrice SOAP
     * method.
     * 
     *
     * @param storeView
     * @return
     */
    CatalogProductReturnEntity getProductSpecialPrice(@NotNull ProductIdentifier productId, String storeView)
        throws ExceptionType;

    /**
     * Answers a product's specified attributes. At least one of attributNames or
     * additionalAttributeNames must be non null and non empty. See
     * catalog-product-info SOAP method.
     * 
     *
     * @param productId
     * @param storeView
     * @param attributeNames the list of standard attributes to be returned
     * @param additionalAttributeNames the list of non standard attributes to be
     *            returned in the additionalAttributes attribute
     * @return the product attributes
     */
    CatalogProductReturnEntity getProduct(@NotNull ProductIdentifier productId,
                                          String storeView,
                                          List<String> attributeNames,
                                          List<String> additionalAttributeNames) throws ExceptionType;

    /**
     * Retrieve products list by filters See catalog-product-list SOAP method.
     * 
     *
     * @param filters an optional filtering expression
     * @return the list of product attributes that match the given optional filtering
     *         expression
     */
    List<CatalogProductEntity> listProducts(String filters, String storeView) throws ExceptionType;

    /**
     * Sets a product special price. See catalog-product-setSpecialPrice SOAP method
     * 
     *
     * @param specialPrice
     * @param fromDate
     * @param toDate
     * @param storeView
     * @throws ExceptionType
     */
    int updateProductSpecialPrice(@NotNull ProductIdentifier productId,
                                  @NotNull String specialPrice,
                                  String fromDate,
                                  String toDate,
                                  String storeView) throws ExceptionType;

    /**
     * Updates a product. At least one of attributes or additionalAttributes 
     * must be non null and non empty See catalog-category-updateProduct SOAP method
     *
     * @param attributes standard product attributes to update
     * @param additionalAttributes non standard product attributes to update
     */
    boolean updateProduct(@NotNull ProductIdentifier productId,
                          CatalogProductCreateEntity attributes,
                          List<AssociativeEntity> additionalAttributes,
                          String storeView) throws ExceptionType;

    /**
     * Creates a new product media. See catalog-product-attribute-media-create SOAP
     * method.
     * 
     * @param product
     * @param attributes
     * @param storeView
     * @return the new image filename
     */
    String createProductAttributeMedia(@NotNull ProductIdentifier productId,
                                       CatalogProductAttributeMediaCreateEntity attributes,
                                       @NotNull InputStream content,
                                       @NotNull MediaMimeType mimeType,
                                       @NotNull String baseFileName,
                                       String storeView) throws ExceptionType;

    /**
     * Answers product image attributes. See catalog-product-attribute-media-info
     * SOAP method
     * 
     *
     * @param productId
     * @param file
     * @param storeView
     * @return the product attributes
     */
    CatalogProductImageEntity getProductAttributeMedia(@NotNull ProductIdentifier productId,
                                                       @NotNull String file,
                                                       String storeView) throws ExceptionType;

    /**
     * Retrieves product image list. See catalog-product-attribute-media-list SOAP
     * method
     * 
     *
     * @param storeViewIdOrCode@return the list of product images attributes
     */
    List<CatalogProductImageEntity> listProductAttributeMedia(@NotNull ProductIdentifier productId, String storeViewIdOrCode)
        throws ExceptionType;

    /**
     * Removes a product image. See catalog-product-attribute-media-remove SOAP
     * method.
     * 
     * @param productId
     * @param file
     * @return if the image is removed from a product
     */
    boolean deleteProductAttributeMedia(@NotNull ProductIdentifier productId, @NotNull String file)
        throws ExceptionType;

    /**
     * Retrieve product image types. See catalog-product-attribute-media-types SOAP
     * method.
     * 
     *
     * @param setId
     * @return
     */
    List<CatalogProductAttributeMediaTypeEntity> listProductAttributeMediaTypes(int setId) throws ExceptionType;

    /**
     * Updates product media. See catalog-product-attribute-media-update
     *
     * @param fileName
     * @param attributes
     * @param storeView
     */
    boolean updateProductAttributeMedia(@NotNull ProductIdentifier productId,
                                        String fileName,
                                        @NotNull CatalogProductAttributeMediaCreateEntity attributes,
                                        String storeView) throws ExceptionType;

    /**
     * Retrieves category attributes. See catalog-category-attribute-list SOAP
     * method.
     * 
     * @return the list of attributes
     */
    List<CatalogAttributeEntity> listCategoryAttributes() throws ExceptionType;

    /**
     * Retrieves attribute options. See catalog-category-attribute-options SOAP
     * method.
     * 
     *
     * @param attributeId
     * @return the list of attributes
     */
    List<CatalogAttributeOptionEntity> listCategoryAttributeOptions(@NotNull String attributeId, String storeView)
        throws ExceptionType;

    /**
     * Retrieves product attributes list. See catalog-product-attribute-list SOAP
     * methods
     * 
     *
     * @param setId
     * @return the list of product attributes
     */
    List<CatalogAttributeEntity> listProductAttributes(int setId) throws ExceptionType;

    /**
     * Answers the product attribute options. See catalog-product-attribute-options
     * SOAP method.
     * 
     *
     * @param attributeId
     * @return the attributes list
     */
    @NotNull
    List<CatalogAttributeOptionEntity> listProductAttributeOptions(@NotNull String attributeId, String storeView)
        throws ExceptionType;

    /**
     * Retrieves product attribute sets. See catalog-product-attribute-set-list SOAP
     * method.
     * 
     * @return The list of product attributes sets
     */
    @NotNull
    List<CatalogProductAttributeSetEntity> listProductAttributeSets() throws ExceptionType;

    /**
     * Retrieves product types. See catalog-product-type-list SOAP method
     * 
     * @return the list of product types
     */
    @NotNull
    List<CatalogProductTypeEntity> listProductTypes() throws ExceptionType;

    /**
     * Retrieve product tier prices. See catalog-product-attribute-tier-price-info
     * SOAP Method.
     * 
     *
     * @param productId
     * @return the list of product attributes
     */
    @NotNull
    List<CatalogProductTierPriceEntity> listProductAttributeTierPrices(@NotNull ProductIdentifier productId)
        throws ExceptionType;

    /**
     * Updates a single product tier price. See catalog-product-attribute-tier-price-update
     * SOAP method.
     *
     * @param productId
     * @param attributes
     */
    int updateProductAttributeTierPrice(@NotNull ProductIdentifier productId,
                                        @NotNull CatalogProductTierPriceEntity attributes) throws ExceptionType;

    /**
     * Links two products
     *
     * @param type the product type
     * @param productId
     * @param linkedProductIdOrSku
     * @param attributes the link attributes
     */
    boolean addProductLink(@NotNull String type,
                           @NotNull ProductIdentifier productId,
                           @NotNull String linkedProductIdOrSku,
                           CatalogProductLinkEntity attributes) throws ExceptionType;

    /**
     * Lists all the attributes for the given product link type
     * 
     *
     * @param type the product type
     * @return the listing of product attributes
     */
    List<CatalogProductLinkAttributeEntity> listProductLinkAttributes(@NotNull String type) throws ExceptionType;

    /**
     * Lists linked products to the given product and for the given link type
     * 
     *
     * @param type the link type
     * @param productId the linked product
     * @return the list of links to the product
     */
    List<CatalogProductLinkEntity> listProductLink(@NotNull String type, @NotNull ProductIdentifier productId)
        throws ExceptionType;

    /**
     * Deletes a product's link
     * 
     * @param type link type
     * @param productId
     * @param linkedProductIdOrSku
     * @return if the link is removed from a product
     */
    boolean deleteProductLink(@NotNull String type,
                           @NotNull ProductIdentifier productId,
                           @NotNull String linkedProductIdOrSku) throws ExceptionType;

    /**
     * Retrieve product link types
     * 
     * @return the list of product link types
     */
    List<String> listProductLinkTypes() throws ExceptionType;

    /**
     * Update product link
     *
     * @param type
     * @param linkedProduct
     * @param attributes
     */
    boolean updateProductLink(@NotNull String type,
                              @NotNull ProductIdentifier productId,
                              @NotNull String linkedProduct,
                              @NotNull CatalogProductLinkEntity attributes) throws ExceptionType;

}

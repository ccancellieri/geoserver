/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

/**
 * The WCS 1.1 grid type enumeration
 * 
 * @author Andrea Aime
 * 
 */
public enum GridType {
    GT2dGridIn2dCrs("urn:ogc:def:method:WCS:1.1:2dGridIn2dCrs", 2, 4), //
    GT2dGridIn3dCrs("urn:ogc:def:method:WCS:1.1:2dGridIn3dCrs", 3, 6), //
    GT2dSimpleGrid("urn:ogc:def:method:WCS:1.1:2dSimpleGrid", 2, 2);

    private String xmlConstant;
    private int offsetArrayLength;
    private int originArrayLength;

    GridType(String xmlConstant, int originArrayLenght, int offsetArrayLenght) {
        this.xmlConstant = xmlConstant;
        this.offsetArrayLength = offsetArrayLenght;
        this.originArrayLength = originArrayLenght;
    }

    /**
     * Returns the full fledges xml constant associated to the specified grid type
     * @return
     */
    public String getXmlConstant() {
        return xmlConstant;
    }

    /**
     * Returns the expected size of the offsets array for this grid type
     * @return
     */
    public int getOffsetArrayLength() {
        return offsetArrayLength;
    }

    
    /**
     * Returns the expected size of the origin array for this grid type
     * @return
     */
    public int getOriginArrayLength() {
        return originArrayLength;
    }
    
    
}

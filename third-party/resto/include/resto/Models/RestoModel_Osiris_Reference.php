<?php

class RestoModel_Osiris_Reference extends RestoModel
{

    /*
     * Properties mapping between RESTo model and input GeoJSON Feature file
     * 'propertyNameInInputFile' => 'restoPropertyName'
     */
    public $inputMapping = array(
        'properties.productIdentifier' => 'productIdentifier',
        'properties.owner'             => 'owner',
        'properties.filename'          => 'filename',
        'properties.osirisUrl'           => 'osirisUrl',
        'properties.resource'          => 'resource',
        'properties.resourceMimeType'  => 'resourceMimeType',
        'properties.resourceSize'      => 'resourceSize',
        'properties.resourceChecksum'  => 'resourceChecksum',
        'properties.extraParams'       => 'osirisparam'
    );

    public $extendedProperties = array(
        'owner'     => array(
            'name' => 'owner',
            'type' => 'TEXT'
        ),
        'filename'  => array(
            'name' => 'filename',
            'type' => 'TEXT'
        ),
        'osirisUrl'   => array(
            'name' => 'osirisurl',
            'type' => 'TEXT'
        ),
        'osirisparam' => array(
            'name' => 'osirisparam',
            'type' => 'JSONB'
        ),
    );

    public $extendedSearchFilters = array(
        'productIdentifier' => array(
            'name'      => 'productIdentifier',
            'type'      => 'TEXT',
            'osKey'     => 'productIdentifier',
            'key'       => 'productIdentifier',
            'operation' => '=',
            'title'     => 'Identifier of the reference data',
        ),
        'owner'             => array(
            'name'      => 'owner',
            'type'      => 'TEXT',
            'osKey'     => 'owner',
            'key'       => 'owner',
            'operation' => '=',
            'title'     => 'Owner of the reference data',
        ),
        'filename'          => array(
            'name'      => 'filename',
            'type'      => 'TEXT',
            'osKey'     => 'filename',
            'key'       => 'filename',
            'operation' => '=',
            'title'     => 'Reference data filename',
        ),
        'osirisparam'         => array(
            'name'      => 'osirisparam',
            'type'      => 'JSONB',
            'osKey'     => 'osirisparam',
            'key'       => 'osirisparam',
            'operation' => '@>',
        ),
    );

    /*
    * Return property database column name
    *
    * @param string $modelKey : RESTo model key
    * @return array
    */
    public function getDbKey($modelKey)
    {
        if (!isset($modelKey, $this->properties[$modelKey]) || !is_array($this->properties[$modelKey])) {
            return null;
        }
        return $this->properties[$modelKey]['name'];
    }

    /**
     * Constructor
     */
    public function __construct()
    {
        parent::__construct();
        $this->searchFilters = array_merge($this->searchFilters, $this->extendedSearchFilters);
    }

}

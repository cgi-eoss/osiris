<?php

/**
 * Class RestoModel_Osiris_Output
 *
 * @property array $extendedProperties
 */
class RestoModel_Osiris_Output extends RestoModel
{

    /*
     * Properties mapping between RESTo model and input GeoJSON Feature file
     * 'propertyNameInInputFile' => 'restoPropertyName'
     */
    public $inputMapping = array(
        'properties.productIdentifier' => 'productIdentifier',
        'properties.jobId'             => 'jobId',
        'properties.intJobId'          => 'intJobId',
        'properties.serviceName'       => 'serviceName',
        'properties.jobOwner'          => 'jobOwner',
        'properties.jobStartTime'      => 'jobStartDate',
        'properties.jobEndTime'        => 'jobEndDate',
        'properties.filename'          => 'filename',
        'properties.osirisUrl'           => 'osirisUrl',
        'properties.resource'          => 'resource',
        'properties.resourceMimeType'  => 'resourceMimeType',
        'properties.resourceSize'      => 'resourceSize',
        'properties.resourceChecksum'  => 'resourceChecksum',
        'properties.extraParams'       => 'osirisparam'
    );

    public $extendedProperties = array(
        'jobId'        => array(
            'name' => 'jobId',
            'type' => 'TEXT'
        ),
        'intJobId'     => array(
            'name' => 'intJobId',
            'type' => 'INTEGER'
        ),
        'serviceName'  => array(
            'name' => 'serviceName',
            'type' => 'TEXT'
        ),
        'jobOwner'     => array(
            'name' => 'jobOwner',
            'type' => 'TEXT'
        ),
        'jobStartDate' => array(
            'name' => 'jobStartDate',
            'type' => 'TIMESTAMP',
        ),
        'jobEndDate'   => array(
            'name' => 'jobEndDate',
            'type' => 'TIMESTAMP',
        ),
        'filename'     => array(
            'name' => 'filename',
            'type' => 'TEXT'
        ),
        'osirisUrl'      => array(
            'name' => 'osirisUrl',
            'type' => 'TEXT'
        ),
        'osirisparam'    => array(
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
            'title'     => 'Identifier of the output product',
        ),
        'jobId'             => array(
            'name'      => 'jobId',
            'type'      => 'TEXT',
            'osKey'     => 'jobId',
            'key'       => 'jobId',
            'operation' => '=',
            'title'     => 'Identifier of the job',
        ),
        'intJobId'          => array(
            'name'      => 'intJobId',
            'type'      => 'INTEGER',
            'osKey'     => 'intJobId',
            'key'       => 'intJobId',
            'operation' => '=',
            'title'     => 'Identifier of the job',
        ),
        'serviceName'       => array(
            'name'      => 'serviceName',
            'type'      => 'TEXT',
            'osKey'     => 'serviceName',
            'key'       => 'serviceName',
            'operation' => '=',
            'title'     => 'Identifier of the serviceName',
        ),
        'jobOwner'          => array(
            'name'      => 'jobOwner',
            'type'      => 'TEXT',
            'osKey'     => 'jobOwner',
            'key'       => 'jobOwner',
            'operation' => '=',
            'title'     => 'Owner of the job',
        ),
        'jobStartDate'      => array(
            'name'  => 'jobStartDate',
            'type'  => 'TIMESTAMP', 
            'osKey'     => 'jobStartDate',
            'key'       => 'jobStartDate',
            'operation' => '>=',
            'title'     => 'Start of job processing',
            'index' => array(
                'type'      => 'btree',
                'direction' => 'DESC'
            )
        ),
        'jobEndDate'        => array(
            'name'  => 'jobEndDate',
            'type'  => 'TIMESTAMP',
            'osKey'     => 'jobEndDate',
            'key'       => 'jobEndDate',
            'operation' => '<=',
            'title'     => 'End of job processing',            
            'index' => array(
                'type'      => 'btree',
                'direction' => 'DESC'
            )
        ),
        'filename'          => array(
            'name'      => 'filename',
            'type'      => 'TEXT',
            'osKey'     => 'filename',
            'key'       => 'filename',
            'operation' => '=',
            'title'     => 'Output product filename',
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

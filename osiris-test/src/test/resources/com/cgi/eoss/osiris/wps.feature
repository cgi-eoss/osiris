Feature: OSIRIS :: WPS

  Scenario: GetCapabilities
    Given OSIRIS WPS is available
    And the default services are loaded
    When a user requests GetCapabilities from WPS
    Then they receive the OSIRIS service list

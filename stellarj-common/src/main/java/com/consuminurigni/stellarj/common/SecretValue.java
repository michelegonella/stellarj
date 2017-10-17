package com.consuminurigni.stellarj.common;

/**
 * Some values should not be printed to log directly - for example database
 * connection strings with password or secret keys.
 *
 * Using this simple wrapper allows to prevent using these values as a normal
 * string as it requires extra code to read the value.
 */
public class SecretValue {

}

package io.openems.edge.meter.openinvertergateway;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.SinglePhase;

@ObjectClassDefinition(//
		name = "Meter OpenInverterGateway", //
		description = "Implements OpenInverterGateway for Growatt Inverters as Meter")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Phase", description = "Which Phase is this Inverter connected to?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Inverter.")
	String ip();

	String webconsole_configurationFactory_nameHint() default "Meter OpenInverterGateway [{id}]";

}
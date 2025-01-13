package io.openems.edge.meter.openinvertergateway;

import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static java.lang.Math.round;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.OpenInverterGateway", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class OpenInverterGatewayImpl extends AbstractOpenemsComponent implements OpenInverterGateway, OpenemsComponent,
		EventHandler, SinglePhaseMeter, ElectricityMeter, TimedataProvider {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(OpenInverterGatewayImpl.class);

	private MeterType meterType = null;
	private SinglePhase phase = null;
	private String baseUrl;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public OpenInverterGatewayImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				OpenInverterGateway.ChannelId.values(), //
				ElectricityMeter.ChannelId.values());

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = MeterType.PRODUCTION;
		this.phase = config.phase();
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/status", this::processHttpResult);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.calculateEnergy();
		}
	}

	private void calculateEnergy() {
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		Integer activePower = null;
		Integer current = null;
		Integer voltage = null;

		if (error != null) {
			this.logWarn(this.log, error.getMessage());
		} else {
			try {
				var response = getAsJsonObject(result.data());

				activePower = round(getAsFloat(response, "OutputPower"));
				current = round(getAsFloat(response, "PV1InputCurrent") * 1000);
				voltage = round(getAsFloat(response, "PV1Voltage") * 1000);

			} catch (Exception e) {
				this.logWarn(this.log, e.getMessage());
			}
		}

		this._setActivePower(activePower);
		this._setCurrent(current);
		this._setVoltage(voltage);
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return this.phase + ": " + this.getActivePower().asString();
	}
}

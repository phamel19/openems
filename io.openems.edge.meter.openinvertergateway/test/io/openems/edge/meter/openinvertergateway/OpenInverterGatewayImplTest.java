package io.openems.edge.meter.openinvertergateway;

import static io.openems.common.types.MeterType.PRODUCTION;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.timedata.test.DummyTimedata;

public class OpenInverterGatewayImplTest {

	@Test
	public void test() throws Exception {

		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new OpenInverterGatewayImpl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory())
				.addReference("timedata", new DummyTimedata("timedata0")).activate(MyConfig.create() //
						.setId("meter0") //
						.setPhase(SinglePhase.L1).setIp("127.0.0.1").setType(PRODUCTION).build()) //
				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									"InverterStatus": 1,
									"InputPower": 27.6,
									"PV1Voltage": 90.9,
									"PV1InputCurrent": 0.2,
									"PV1InputPower": 27.6,
									"OutputPower": 27.1,
									"GridFrequency": 50.02,
									"L1ThreePhaseGridVoltage": 229.8,
									"L1ThreePhaseGridOutputCurrent": 0.2,
									"L1ThreePhaseGridOutputPower": 28.3,
									"TodayGenerateEnergy": 0,
									"TotalGenerateEnergy": 457.5,
									"TWorkTimeTotal": 6220179,
									"PV1EnergyToday": 0,
									"PV1EnergyTotal": 463.1,
									"PVEnergyTotal": 463.1,
									"InverterTemperature": 6.6
									}
									"""));
							httpTestBundle.triggerNextCycle();
						}).onAfterProcessImage(() -> assertEquals("L1: 27 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 27)
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 27)
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null)
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null)
						.output(ElectricityMeter.ChannelId.VOLTAGE, 90900)
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 90900)
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null)
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null)
						.output(ElectricityMeter.ChannelId.CURRENT, 200)
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 200)
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null)
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null)
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null)
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null)
						.output(OpenInverterGateway.ChannelId.SLAVE_COMMUNICATION_FAILED, false))
				.next(new TestCase("Invalid read response").onBeforeProcessImage(() -> {
					httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
					httpTestBundle.triggerNextCycle();
				}).onAfterProcessImage(() -> assertEquals("L1: UNDEFINED", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null)
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null)
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null)
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null)
						.output(ElectricityMeter.ChannelId.VOLTAGE, null)
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, null)
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null)
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null)
						.output(ElectricityMeter.ChannelId.CURRENT, null)
						.output(ElectricityMeter.ChannelId.CURRENT_L1, null)
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null)
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null)
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null)
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 0L)
						.output(OpenInverterGateway.ChannelId.SLAVE_COMMUNICATION_FAILED, true))

				.deactivate();
	}

}

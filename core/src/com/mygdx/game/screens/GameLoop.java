/*
 * Copyright 2010 Mario Zechner (contact@badlogicgames.com), Nathan Sweet (admin@esotericsoftware.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.mappings.Ouya;
import com.mygdx.game.Invaders;
import com.mygdx.game.Renderer;
import com.mygdx.game.simulation.Simulation;
import com.mygdx.game.simulation.SimulationListener;

public class GameLoop extends InvadersScreen implements SimulationListener {
	/** the simulation **/
	private final Simulation simulation;
	/** the renderer **/
	private final Renderer renderer;
	/** explosion sound **/
	private final Sound explosion;
	/** shot sound **/
	private final Sound shot;

	/** controller **/
	private int buttonsPressed = 0;
	private ControllerListener listener = new ControllerAdapter() {
		@Override
		public boolean buttonDown(Controller controller, int buttonIndex) {
			buttonsPressed++;
			return true;
		}

		@Override
		public boolean buttonUp(Controller controller, int buttonIndex) {
			buttonsPressed--;
			return true;
		}
	};

	public GameLoop (Invaders invaders) {
		super(invaders);
		simulation = new Simulation();
		simulation.listener = this;
		renderer = new Renderer();
		explosion = Gdx.audio.newSound(Gdx.files.internal("data/explosion.wav"));
		shot = Gdx.audio.newSound(Gdx.files.internal("data/shot.wav"));

		if (invaders.getController() != null) {
			invaders.getController().addListener(listener);
		}
	}

	@Override
	public void dispose () {
		renderer.dispose();
		shot.dispose();
		explosion.dispose();
		if (invaders.getController() != null) {
			invaders.getController().removeListener(listener);
		}
		simulation.dispose();
	}

	@Override
	public boolean isDone () {
		return simulation.ship.lives == 0;
	}

	@Override
	public void draw (float delta) {
		renderer.render(simulation, delta);
	}

	@Override
	public void update (float delta) {
		simulation.update(delta);

//		float q1 = BLEDeviceScanActivity.latest_Q0;
//		float q2 = BLEDeviceScanActivity.latest_Q1;
//		float q3 = BLEDeviceScanActivity.latest_Q2;
//		float q4 = BLEDeviceScanActivity.latest_Q3;
//
//		//Equation from Omid's paper with conversions to make the math work
//		double pi_d = Math.PI;
//		float pi_f = (float)pi_d;
//
//		double q1_double = q1;
//		double theta_double = 2*Math.acos(q1_double);
//		float theta = (float)theta_double*180/pi_f;
//
//		double q2_double = q2;
//		double rx_double = -1 * q2_double / Math.sin(theta_double/2);
//		float rx = (float)rx_double;
//
//		double q3_double = q3;
//		double ry_double = -1 * q3_double / Math.sin(theta_double/2);
//		float ry = (float)ry_double;
//
//		double q4_double = q4;
//		double rz_double = -1 * q4_double / Math.sin(theta_double/2);
//		float rz = (float)rz_double;
		float q0 = (float) Invaders.mInvaderInterface1.getQ0();
		float q1 = (float) Invaders.mInvaderInterface1.getQ1();
		float q2 = (float) Invaders.mInvaderInterface1.getQ2();
		float q3 = (float) Invaders.mInvaderInterface1.getQ3();

		float q0_2 = (float) Invaders.mInvaderInterface2.getQ0();
		float q1_2 = (float) Invaders.mInvaderInterface2.getQ1();
		float q2_2 = (float) Invaders.mInvaderInterface2.getQ2();
		float q3_2 = (float) Invaders.mInvaderInterface2.getQ3();

		float accelerometerY = Gdx.input.getAccelerometerY();

		if (0.2f * (q0 * q1 + q2 * q3) > 0) {
			simulation.moveShipLeft(delta, Math.abs(0.2f * (q0 * q1 + q2 * q3)));
		} else{
			simulation.moveShipRight(delta, Math.abs(0.2f * (q0 * q1 + q2 * q3)));
		}

		if (0.2f * (q0_2 * q1_2 + q2_2 * q3_2)>0) {
			simulation.moveShipLeft2(delta, Math.abs(0.2f * (q0_2 * q1_2 + q2_2 * q3_2)));
		}else{
			simulation.moveShipRight2(delta,Math.abs(0.2f * (q0_2 * q1_2 + q2_2 * q3_2)));
		}



		if (invaders.getController() != null) {
			if (buttonsPressed > 0) {
				simulation.shot();
				simulation.shot2();
			}

			// if the left stick moved, move the ship
			float axisValue = invaders.getController().getAxis(Ouya.AXIS_LEFT_X) * 0.5f;
			if (Math.abs(axisValue) > 0.25f) {
				if (axisValue > 0) {
					simulation.moveShipRight(delta, axisValue);
					simulation.moveShipRight2(delta, axisValue);
				} else {
					simulation.moveShipLeft(delta, -axisValue);
					simulation.moveShipLeft2(delta, -axisValue);
				}
			}
		}

		if (Gdx.input.isKeyPressed(Keys.DPAD_LEFT) || Gdx.input.isKeyPressed(Keys.A)) simulation.moveShipLeft(delta, 0.5f);
		if (Gdx.input.isKeyPressed(Keys.DPAD_LEFT) || Gdx.input.isKeyPressed(Keys.A)) simulation.moveShipLeft2(delta, 0.5f);

		if (Gdx.input.isKeyPressed(Keys.DPAD_RIGHT) || Gdx.input.isKeyPressed(Keys.D)) simulation.moveShipRight(delta, 0.5f);
		if (Gdx.input.isKeyPressed(Keys.DPAD_RIGHT) || Gdx.input.isKeyPressed(Keys.D)) simulation.moveShipRight2(delta, 0.5f);

		if (Gdx.input.isTouched() || Gdx.input.isKeyPressed(Keys.SPACE)) simulation.shot();
		if (Gdx.input.isTouched() || Gdx.input.isKeyPressed(Keys.SPACE)) simulation.shot2();
	}

	@Override
	public void explosion () {
		explosion.play();
	}

	@Override
	public void shot () {
		shot.play();
	}
}

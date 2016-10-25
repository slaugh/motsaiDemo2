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
		return simulation.ships[0].lives == 0;
	}

	@Override
	public void draw (float delta) {
		renderer.render(simulation, delta);
	}

	@Override
	public void update (float delta) {
		simulation.update(delta);

		float[] q0s = new float[simulation.MAX_SHIPS];
		float[] q1s = new float[simulation.MAX_SHIPS];
		float[] q2s = new float[simulation.MAX_SHIPS];
		float[] q3s = new float[simulation.MAX_SHIPS];

		for(int shipNumber = 0; shipNumber < simulation.MAX_SHIPS; shipNumber++){

			q0s[shipNumber] = (float) Invaders.mInvaderInterfaceArray[shipNumber].getQ0();
			q1s[shipNumber] = (float) Invaders.mInvaderInterfaceArray[shipNumber].getQ1();
			q2s[shipNumber] = (float) Invaders.mInvaderInterfaceArray[shipNumber].getQ2();
			q3s[shipNumber] = (float) Invaders.mInvaderInterfaceArray[shipNumber].getQ3();

		}

		float accelerometerY = Gdx.input.getAccelerometerY();

		for(int shipNumber = 0; shipNumber < simulation.MAX_SHIPS; shipNumber++){

			//This assumps that it is either left or right...
			if (0.2f * (q0s[shipNumber] * q1s[shipNumber] + q2s[shipNumber] * q3s[shipNumber]) > 0) {
				simulation.moveShipLeft(delta, Math.abs(0.2f * (q0s[shipNumber] * q1s[shipNumber] + q2s[shipNumber] * q3s[shipNumber])),shipNumber);
			} else{
				simulation.moveShipRight(delta, Math.abs(0.2f * (q0s[shipNumber] * q1s[shipNumber] + q2s[shipNumber] * q3s[shipNumber])),shipNumber);
			}
		}

		if (invaders.getController() != null) {
			if (buttonsPressed > 0) {
				simulation.shot();
			}

			// if the left stick moved, move the ship
			float axisValue = invaders.getController().getAxis(Ouya.AXIS_LEFT_X) * 0.5f;
			if (Math.abs(axisValue) > 0.25f) {
				if (axisValue > 0) {
					simulation.moveShipRight(delta, axisValue,1);
				} else {
					simulation.moveShipLeft(delta, -axisValue,1);
				}
			}
		}

//		if (Gdx.input.isKeyPressed(Keys.DPAD_LEFT) || Gdx.input.isKeyPressed(Keys.A)) simulation.moveShipLeft(delta, 0.5f);
//		if (Gdx.input.isKeyPressed(Keys.DPAD_LEFT) || Gdx.input.isKeyPressed(Keys.A)) simulation.moveShipLeft2(delta, 0.5f);
//
//		if (Gdx.input.isKeyPressed(Keys.DPAD_RIGHT) || Gdx.input.isKeyPressed(Keys.D)) simulation.moveShipRight(delta, 0.5f);
//		if (Gdx.input.isKeyPressed(Keys.DPAD_RIGHT) || Gdx.input.isKeyPressed(Keys.D)) simulation.moveShipRight2(delta, 0.5f);
//
		if (Gdx.input.isTouched() || Gdx.input.isKeyPressed(Keys.SPACE)) simulation.shot();
//		if (Gdx.input.isTouched() || Gdx.input.isKeyPressed(Keys.SPACE)) simulation.shot2();
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

/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
import tests from './tests.js';

class TestBench {

	selector;

	launcher;

	runner;

	render = async engine => {
		if (engine.isRendering(this))
			return engine.render(this, 'TestBench');

		if (engine.isRendering(this, 'launcher')) {
			this.launcher = new Launcher();
			this.launcher.selector = () => this.selector().firstElementChild;
			return this.launcher;
		}

		if (engine.isRendering(this, 'runner')) {
			this.runner = new Runner();
			this.runner.selector = () => this.selector().lastElementChild;
			return this.runner;
		}
	}

	listen = () => {
		this.selector().addEventListener('testsuitelaunch', this.handleTestSuiteLaunch);
		this.selector().addEventListener('testcomplete', this.handleTestComplete);
		this.launcher.listen();
		this.runner.listen();
	}

	handleTestSuiteLaunch = e => {
		this.launcher.selector().querySelectorAll('[data-key]').forEach(e => e.className = '');
		this.runner.keys = e.detail.tests;
		this.runner.refresh();
	}

	handleTestComplete = e => {
		const f = this.launcher.selector().querySelector(`[data-key="${e.detail.key}"]`);
		if (e.detail.error) {
			f.className = 'failed';
			f.setAttribute('title', e.detail.error);
		} else {
			f.className = 'succeeded';
			f.removeAttribute('title');
		}
	}
}

class Launcher {

	selector;

	get items() {
		return Object.keys(tests);
	}

	render = async engine => {
		if (engine.isRendering(this))
			return engine.render(this, 'TestBench-Launcher');

		if (engine.isRendering(this, 'items', true))
			return await engine.render(engine.target, 'TestBench-Launcher-item');
	}

	listen = () => {
		this.selector().querySelector('input').addEventListener('change', this.handleInputChange);
		this.selector().querySelector('form').addEventListener('submit', this.handleFormSubmit);
	}

	handleInputChange = e => {
		const c = e.currentTarget.checked;
		this.selector().querySelectorAll('[data-key] input').forEach(f => f.checked = c);
	}

	handleFormSubmit = async e => {
		e.preventDefault();
		const f = e.currentTarget;
		this.selector().dispatchEvent(new CustomEvent('testsuitelaunch', {
			bubbles: true,
			detail: { tests: new FormData(f).getAll('test') }
		}));
	}
}

class Runner {

	selector;

	engine;

	keys;

	render = async engine => {
		if (engine.isRendering(this)) {
			this.engine = engine.clone();
			return engine.render(this, 'TestBench-Runner');
		}

		if (engine.isRendering(this, 'iframe')) {
			if (!this.keys?.length)
				return null;
			await fetch('/test/start', { method: 'POST' });
			localStorage.removeItem('jwtToken');
			return engine.render(this, 'TestBench-Runner-iframe');
		}
	}

	listen = () => {
		this.selector().querySelector('iframe')?.addEventListener('load', this.handleIframeLoad);
	}

	refresh = async () => {
		this.selector().outerHTML = await this.render(this.engine);
		this.listen();
	}

	handleIframeLoad = async e => {
		const i = e.currentTarget;
		const k = this.keys.shift();
		const t = tests[k];
		t(i.contentDocument).then(async () => {
			await new Promise(x => setTimeout(x, 200));
			this.selector().dispatchEvent(new CustomEvent('testcomplete', {
				bubbles: true,
				detail: { key: k }
			}));
		}).catch(async f => {
			await new Promise(x => setTimeout(x, 200));
			this.selector().dispatchEvent(new CustomEvent('testcomplete', {
				bubbles: true,
				detail: {
					key: k,
					error: f
				}
			}));
		}).finally(async () => {
			await fetch('/test/stop', { method: 'POST' });
			await this.refresh();
		});
	}
}

export default TestBench;

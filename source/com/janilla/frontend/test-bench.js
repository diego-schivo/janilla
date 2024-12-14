import { UpdatableElement } from "./web-components.js";
import tests from './tests.js';

export default class TestBench extends UpdatableElement {

	static get observedAttributes() {
		return ["data-values"];
	}

	static get templateName() {
		return "test-bench";
	}
	
	state = Object.entries(tests).map((x, i) => ({
		value: i,
		text: x[0]
	}));

	constructor() {
		super();
	}

	connectedCallback() {
		// console.log("TestBench.connectedCallback");
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		// console.log("TestBench.disconnectedCallback");
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("submit", this.handleSubmit);
	}

	handleChange = event => {
		// console.log("TestBench.handleChange", event);
		if (!event.target.matches(":not([name])"))
			return;
		this.querySelectorAll('[name="test"]').forEach(x => x.checked = event.target.checked);
	}

	handleLoad = event => {
		// console.log("TestBench.handleLoad", event);
		const k = this.keys.shift();
		const s = this.state[k];
		tests[s.text](event.target.contentDocument).then(async () => {
			await new Promise(x => setTimeout(x, 200));
			s.class = "succeeded";
		}).catch(async error => {
			await new Promise(x => setTimeout(x, 200));
			s.class = "failed";
			s.title = error;
		}).finally(async () => {
			await fetch('/test/stop', { method: 'POST' });
			this.requestUpdate();
		});
	}

	handleSubmit = event => {
		// console.log("TestBench.handleSubmit", event);
		event.preventDefault();
		this.keys = new FormData(event.target).getAll('test');
		this.state.forEach(x => x.class = null);
		this.requestUpdate();
	}

	async update() {
		// console.log("TestBench.update");
		await super.update();
		this.interpolator ??= this.interpolatorBuilders[0]();
		if (this.items?.length !== this.state.length)
			this.items = this.state.map(_ => this.interpolatorBuilders[1]());
		this.iframe ??= this.interpolatorBuilders[2]();
		if (this.keys?.length) {
			await fetch('/test/start', { method: 'POST' });
			localStorage.removeItem('jwtToken');
		}
		this.appendChild(this.interpolator({
			items: this.items.map((x, i) => x(this.state[i])),
			iframe: this.keys?.length ? this.iframe() : null
		}));
		this.querySelector("iframe")?.addEventListener("load", this.handleLoad);
	}
}

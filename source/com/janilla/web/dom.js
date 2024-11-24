/*
const placeholder = RegExp("\\$\\{(.*?)\\}", "g");

export function interpolate(target, data) {
	if (typeof target === "string") {
		if (target.includes("${")) {
			let a, i = 0, t = [];
			while ((a = placeholder.exec(target)) !== null) {
				if (a.index > i)
					t.push(target.substring(i, a.index));
				let o = data;
				for (const n of a[1].split("."))
					if (o != null && n)
						o = o[n];
					else
						break;
				if (typeof o === "number")
					o = o.toString();
				if (o != null)
					Array.isArray(o) ? t.push(...o) : t.push(o);
				i = placeholder.lastIndex;
			}
			if (i < target.length)
				t.push(target.substring(i));
			target = !t.length ? undefined : t.every(x => typeof x === "string") ? t.join("") : t;
		}
	} else if (target instanceof Text) {
		if (target.nodeValue.includes("${")) {
			const r = interpolate(target.nodeValue, data);
			if (r == null) {
				target.remove();
				target = undefined;
			} else if (typeof r === "string")
				target.nodeValue = r;
			else {
				const t = r.map(x => typeof x === "string" ? new Text(x) : x);
				target.replaceWith(...t);
				target = t;
			}
		}
	} else if (target instanceof Comment) {
		if (target.nodeValue.includes("${")) {
			const r = interpolate(target.nodeValue, data);
			if (r == null) {
				target.remove();
				target = undefined;
			} else if (typeof r === "string")
				target.nodeValue = r;
			else {
				const t = r.map(x => typeof x === "string" ? new Comment(x) : x);
				target.replaceWith(...t);
				target = t;
			}
		}
	} else {
		if (target instanceof Element && target.hasAttributes())
			for (const a of target.attributes)
				if (a.value.includes("${")) {
					const r = interpolate(a.value, data);
					if (typeof r !== "undefined")
						a.value = r;
					else
						target.removeAttribute(a.name);
				}
		if (target.hasChildNodes())
			for (const n of target.childNodes)
				interpolate(n, data);
	}
	return target;
}
*/

export function buildInterpolator(node) {
	let n = node;
	const ff = [];
	const e = (expression, context) => {
		let r = context;
		for (const k of expression.split("."))
			if (r != null && k)
				r = r[k];
			else
				break;
		return r;
	};
	while (n) {
		if (n instanceof Text) {
			if (n.nodeValue.startsWith("${") && n.nodeValue.endsWith("}")) {
				const k = n.nodeValue.substring(2, n.nodeValue.length - 1);
				n.nodeValue = "";
				const o = n;
				ff.push(x => o.nodeValue = e(k, x));
			}
		} else if (n instanceof Comment) {
			if (n.nodeValue.startsWith("${") && n.nodeValue.endsWith("}")) {
				const k = n.nodeValue.substring(2, n.nodeValue.length - 1);
				n.nodeValue = "";
				const o = n;
				let l = 0;
				ff.push(x => {
					while (l > 0) {
						o.parentNode.removeChild(o.nextSibling);
						l--;
					}
					const y = e(k, x);
					if (y == null)
						return;
					const ns = o.nextSibling;
					(Array.isArray(y) ? y : [y]).forEach(z => {
						o.parentNode.insertBefore(z, ns);
					});
					for (let z = o.nextSibling; z !== ns; z = z.nextSibling)
						l++;
				});
			}
		} else if (n instanceof Element && n.hasAttributes())
			for (const a of n.attributes)
				if (a.value.startsWith("${") && a.value.endsWith("}")) {
					const k = a.value.substring(2, a.value.length - 1);
					a.value = "";
					ff.push(x => a.value = e(k, x));
				}
		if (n.firstChild) {
			n = n.firstChild;
			continue;
		}
		do {
			if (n.nextSibling) {
				n = n.nextSibling;
				break;
			}
			n = n.parentNode;
		} while (n);
	}
	return x => {
		ff.forEach(f => f(x));
		return node;
	};
}

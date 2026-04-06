/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.websitetemplate.backend;

import java.util.function.Predicate;

import com.janilla.backend.cms.AbstractCollectionApi;
import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.smtp.SmtpClient;
import com.janilla.http.HttpExchange;
import com.janilla.web.Handle;
import com.janilla.websitetemplate.FormSubmission;
import com.janilla.websitetemplate.WebsiteDomain;

@Handle(path = "/api/form-submissions")
public class FormSubmissionApi extends AbstractCollectionApi<Long, FormSubmission> {

	protected final WebsiteDomain domain;

	protected final SmtpClient smtpClient;

	public FormSubmissionApi(Predicate<HttpExchange> drafts, Persistence persistence, WebsiteDomain domain,
			SmtpClient smtpClient) {
		super(FormSubmission.class, drafts, persistence, "title");
		this.domain = domain;
		this.smtpClient = smtpClient;
	}

//	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(.*?)\\}");

	@Override
	@Handle(method = "POST")
	public FormSubmission create(FormSubmission document) {
//		var d = super.create(document);
//		var m = d.submissionData().stream().collect(Collectors.toMap(SubmissionDatum::field, SubmissionDatum::value));
//		var f = persistence.crud(Form.class).read(d.form());
//		if (f.confirmationType().equals(domain.formConfirmationType("MESSAGE")))
//			for (var e : f.emails()) {
//				var ss = List.of(e.emailFrom(), e.emailTo(), e.subject(), e.message()).stream()
//						.map(x -> PLACEHOLDER.matcher(x).replaceAll(y -> m.get(y.group(1)))).toList();
//				smtpClient.sendMail(OffsetDateTime.now(), ss.get(0), ss.get(1), ss.get(2), ss.get(3));
//			}
//		return d;
		throw new RuntimeException();
	}
}

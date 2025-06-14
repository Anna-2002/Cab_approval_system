const express = require("express");
const cors = require("cors");
const bodyParser = require("body-parser");
const SibApiV3Sdk = require("sib-api-v3-sdk");
require("dotenv").config(); // Optional: to support local .env files

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(bodyParser.json());

// Use Brevo (Sendinblue) API key from environment variable
const apiKey = process.env.BREVO_API_KEY;
if (!apiKey) {
  console.error("❌ BREVO_API_KEY not set in environment variables!");
  process.exit(1);
}

const defaultClient = SibApiV3Sdk.ApiClient.instance;
defaultClient.authentications['api-key'].apiKey = apiKey;

app.post("/send-mail", async (req, res) => {
  const { requesterEmail, approverEmail, requesterName } = req.body;

  if (!requesterEmail || !approverEmail || !requesterName) {
    return res.status(400).json({ message: "Missing required fields" });
  }

  const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();

  try {
    const emailToRequester = {
      sender: { name: "Cab Approval System", email: "noreply@yourdomain.com" },
      to: [{ email: requesterEmail }],
      subject: "Cab Request Submitted",
      htmlContent: `<p>Your cab request has been submitted successfully.</p><p><b>Status:</b> Approval Pending - Functional Head</p>`,
    };

    const emailToApprover = {
      sender: { name: "Cab Approval System", email: "noreply@yourdomain.com" },
      to: [{ email: approverEmail }],
      subject: "Cab Request Approval Pending",
      htmlContent: `<p>You have a new cab request pending approval for <b>${requesterName}</b>.</p>`,
    };

    await apiInstance.sendTransacEmail(emailToRequester);
    await apiInstance.sendTransacEmail(emailToApprover);

    res.status(200).json({ message: "Emails sent successfully" });
  } catch (error) {
    console.error("Email error:", error);
    res.status(500).json({ error: "Failed to send email" });
  }
});

app.get("/", (req, res) => {
  res.send("Cab Email Service is running.");
});

app.listen(PORT, () => {
  console.log(`🚀 Server running on port ${PORT}`);
});
=======
const express = require("express");
const cors = require("cors");
const bodyParser = require("body-parser");
const SibApiV3Sdk = require("sib-api-v3-sdk");
require("dotenv").config(); // Optional: to support local .env files

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(bodyParser.json());

// Use Brevo (Sendinblue) API key from environment variable
const apiKey = process.env.BREVO_API_KEY;
if (!apiKey) {
  console.error("❌ BREVO_API_KEY not set in environment variables!");
  process.exit(1);
}

const defaultClient = SibApiV3Sdk.ApiClient.instance;
defaultClient.authentications['api-key'].apiKey = apiKey;

app.post("/send-mail", async (req, res) => {
  const { requesterEmail, approverEmail, requesterName } = req.body;

  if (!requesterEmail || !approverEmail || !requesterName) {
    return res.status(400).json({ message: "Missing required fields" });
  }

  const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();

  try {
    const emailToRequester = {
      sender: { name: "Cab Approval System", email: "noreply@yourdomain.com" },
      to: [{ email: requesterEmail }],
      subject: "Cab Request Submitted",
      htmlContent: `<p>Your cab request has been submitted successfully.</p><p><b>Status:</b> Approval Pending - Functional Head</p>`,
    };

    const emailToApprover = {
      sender: { name: "Cab Approval System", email: "noreply@yourdomain.com" },
      to: [{ email: approverEmail }],
      subject: "Cab Request Approval Pending",
      htmlContent: `<p>You have a new cab request pending approval for <b>${requesterName}</b>.</p>`,
    };

    await apiInstance.sendTransacEmail(emailToRequester);
    await apiInstance.sendTransacEmail(emailToApprover);

    res.status(200).json({ message: "Emails sent successfully" });
  } catch (error) {
    console.error("Email error:", error);
    res.status(500).json({ error: "Failed to send email" });
  }
});

app.get("/", (req, res) => {
  res.send("Cab Email Service is running.");
});

app.listen(PORT, () => {
  console.log(`🚀 Server running on port ${PORT}`);
});

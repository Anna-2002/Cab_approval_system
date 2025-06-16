const express = require('express');
const SibApiV3Sdk = require('sib-api-v3-sdk');
require('dotenv').config();

const app = express();
app.use(express.json());

// Configure Brevo
const defaultClient = SibApiV3Sdk.ApiClient.instance;
const apiKey = defaultClient.authentications['api-key'];
apiKey.apiKey = process.env.BREVO_API_KEY;
const senderEmail = process.env.SENDER_EMAIL;

app.post('/send-ride-email', async (req, res) => {
    const { 
        requesterEmail,
        requesterName,
        approverEmail,
        approverName,
        approverRole,
        requestId 
    } = req.body;

    // Log incoming request data
    console.log(`[EMAIL NOTIFY] Ride request received.`);
    console.log(`[EMAIL NOTIFY] Request ID: ${requestId}`);
    console.log(`[EMAIL NOTIFY] Requester Email: ${requesterEmail}`);
    console.log(`[EMAIL NOTIFY] Approver Email: ${approverEmail}`);
    console.log(`[EMAIL NOTIFY] Approver name: ${approverName}`);
    console.log(`[EMAIL NOTIFY] Requester name: ${requesterName}`);
    console.log(`[EMAIL NOTIFY] Approver role: ${approverRole}`);

    try {
        // Email to Requester
        const requesterEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        requesterEmailObj.sender = { 
            name: "Cab Approval System", 
            email: senderEmail 
        };
        requesterEmailObj.to = [{ email: requesterEmail }];
        requesterEmailObj.subject = `Ride Request #${requestId} Submitted`;
        requesterEmailObj.htmlContent = `
           <h3>Your Ride Request Has Been Submitted</h3>
            <p>Hello ${requesterName},</p>
            <p>Request ID: ${requestId}</p>
            <p>We've notified ${approverName} (${approverRole}) and will update you once it's reviewed.</p>
        `;

        // Email to Approver
        const approverEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        approverEmailObj.sender = { 
            name: "Cab Approval System", 
            email: senderEmail 
        };
        approverEmailObj.to = [{ email: approverEmail }];
        approverEmailObj.subject = `Approval Needed for Ride Request #${requestId}`;
        approverEmailObj.htmlContent = `
           <h3>New Ride Request Requires Approval</h3>
            <p>Hello ${approverName},</p>
            <p>Request ID: ${requestId} from ${requesterName} needs your approval as ${approverRole}.</p>
            <p><a href="YOUR_APP_URL/approval/${requestId}">Review Request</a></p>
        `;

        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();

        // Send to requester and log
        await apiInstance.sendTransacEmail(requesterEmailObj);
        console.log(`[EMAIL NOTIFY] Requester email sent to: ${requesterEmail}`);

        // Send to approver and log
        await apiInstance.sendTransacEmail(approverEmailObj);
        console.log(`[EMAIL NOTIFY] Approver email sent to: ${approverEmail}`);

        res.status(200).send('Emails sent successfully');
    } catch (error) {
        console.error('[EMAIL NOTIFY] Brevo API Error:', error);
        res.status(500).send('Error sending emails');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));

const express = require('express');
const SibApiV3Sdk = require('sib-api-v3-sdk');
require('dotenv').config();

const app = express();
app.use(express.json());

// Configure Brevo
const defaultClient = SibApiV3Sdk.ApiClient.instance;
const apiKey = defaultClient.authentications['api-key'];
apiKey.apiKey = process.env.BREVO_API_KEY;

app.post('/send-ride-email', async (req, res) => {
    const { requesterEmail, approverEmail, requestId } = req.body;
    
    try {
        // Email to Requester
        const requesterEmail = new SibApiV3Sdk.SendSmtpEmail();
        requesterEmail.sender = { 
            name: "Cab Approval System", 
            email: "noreply@cabapproval.com" 
        };
        requesterEmail.to = [{ email: requesterEmail }];
        requesterEmail.subject = `Ride Request #${requestId} Submitted`;
        requesterEmail.htmlContent = `
            <h3>Your Ride Request Has Been Submitted</h3>
            <p>Request ID: ${requestId}</p>
            <p>We've notified your approver and will update you once it's reviewed.</p>
        `;

        // Email to Approver
        const approverEmail = new SibApiV3Sdk.SendSmtpEmail();
        approverEmail.sender = { 
            name: "Cab Approval System", 
            email: "noreply@cabapproval.com" 
        };
        approverEmail.to = [{ email: approverEmail }];
        approverEmail.subject = `Approval Needed for Ride Request #${requestId}`;
        approverEmail.htmlContent = `
            <h3>New Ride Request Requires Approval</h3>
            <p>Request ID: ${requestId}</p>
            <p>Please review this request at your earliest convenience.</p>
            <p><a href="YOUR_APP_URL/approval/${requestId}">Review Request</a></p>
        `;

        // Send both emails
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();
        await apiInstance.sendTransacEmail(requesterEmail);
        await apiInstance.sendTransacEmail(approverEmail);
        
        res.status(200).send('Emails sent successfully');
    } catch (error) {
        console.error('Brevo API Error:', error);
        res.status(500).send('Error sending emails');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));

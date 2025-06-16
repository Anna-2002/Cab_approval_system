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

// Add startup validation and logging
const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
    console.log(`[SERVER] Running on port ${PORT}`);
    console.log(`[SERVER] Brevo API Key: ${process.env.BREVO_API_KEY ? 'Exists' : 'MISSING!'}`);
    console.log(`[SERVER] Sender Email: ${process.env.SENDER_EMAIL || 'NOT SET!'}`);
    console.log(`[SERVER] Service URL: https://cab-approval-system.onrender.com`);
});

app.post('/send-ride-email', async (req, res) => {
    console.log(`[EMAIL NOTIFY] Endpoint hit at ${new Date().toISOString()}`);
    
    const { 
        requesterEmail,
        requesterName,
        approverEmail,
        approverName,
        approverRole,
        requestId 
    } = req.body;

    // Validate required fields
    if (!requesterEmail || !approverEmail || !requestId) {
        console.log(`[EMAIL NOTIFY] Missing required fields`);
        return res.status(400).json({ error: 'Missing required fields' });
    }

    // Log incoming request data
    console.log(`[EMAIL NOTIFY] Request ID: ${requestId}`);
    console.log(`[EMAIL NOTIFY] Requester: ${requesterEmail} (${requesterName})`);
    console.log(`[EMAIL NOTIFY] Approver: ${approverEmail} (${approverName}) - ${approverRole}`);

    try {
        // Validate environment variables
        if (!process.env.BREVO_API_KEY || !senderEmail) {
            throw new Error('Missing Brevo configuration');
        }

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
            <p>Hello ${requesterName || 'User'},</p>
            <p>Request ID: ${requestId}</p>
            <p>We've notified ${approverName || 'your approver'} (${approverRole || 'Approver'}) and will update you once it's reviewed.</p>
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
            <p>Hello ${approverName || 'Approver'},</p>
            <p>Request ID: ${requestId} from ${requesterName || 'User'} needs your approval as ${approverRole || 'Approver'}.</p>
            <p>Please review this request at your earliest convenience.</p>
        `;

        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();

        console.log(`[EMAIL NOTIFY] Sending emails...`);

        // Send to requester
        await apiInstance.sendTransacEmail(requesterEmailObj);
        console.log(`[EMAIL NOTIFY] ✅ Requester email sent to: ${requesterEmail}`);

        // Send to approver
        await apiInstance.sendTransacEmail(approverEmailObj);
        console.log(`[EMAIL NOTIFY] ✅ Approver email sent to: ${approverEmail}`);

        res.status(200).json({ 
            success: true, 
            message: 'Emails sent successfully',
            requestId: requestId
        });

    } catch (error) {
        console.error('[EMAIL NOTIFY] ❌ Error:', error.message);
        console.error('[EMAIL NOTIFY] Full error:', error);
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send emails',
            details: error.message 
        });
    }
});

// Health check endpoint
app.get('/', (req, res) => {
    res.json({ 
        status: 'running',
        timestamp: new Date().toISOString(),
        service: 'Email Notification Service'
    });
});

// Catch-all for debugging
app.use('*', (req, res) => {
    console.log(`[SERVER] Unhandled request: ${req.method} ${req.originalUrl}`);
    res.status(404).json({ error: 'Endpoint not found' });
});

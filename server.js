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
});

//initial stage email notification to FH and requester
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
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send emails',
            details: error.message 
        });
    }
});

//Email notification to HR and requester on FH approval 
app.post('/send-fh-approval-email', async (req, res) => {
    console.log(`[FH APPROVAL] Endpoint hit at ${new Date().toISOString()}`);
    console.log(`Sending mail to HR and Requester on approval by FH`);
    console.log(`[EMAIL NOTIFY] ✅ Requester email sent to: ${requesterEmail}`);
    console.log(`[EMAIL NOTIFY] ✅ HR email sent to: ${hrEmail}`);
    
    const { 
        requesterEmail,
        requesterName,
        approverEmail,
        approverName,
        requestId,
        empId,
        hrEmail,
        hrName
    } = req.body;

     if (!hrEmail) {
        return res.status(400).json({ error: 'HR email not found for this FH' });
    }

    if (!requesterEmail || !approverEmail || !requestId || !hrEmail) {
        return res.status(400).json({ error: 'Missing required fields' });
    }

    try {
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();

        // Email to Requester
        const requesterEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        requesterEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        requesterEmailObj.to = [{ email: requesterEmail }];
        requesterEmailObj.subject = `✅ Ride Request #${requestId} Approved by FH`;
        requesterEmailObj.htmlContent = `
            <h3>Your Ride Request Has Been Approved by Functional Head</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab request (ID: <strong>${requestId}</strong>) has been approved by ${approverName} (FH).</p>
            <p>It will now be processed by HR for final approval.</p>
        `;

       const hrEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        hrEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        hrEmailObj.to = [{ email: hrEmail }];
        hrEmailObj.subject = `HR Approval Needed for Ride Request #${requestId}`;
        hrEmailObj.htmlContent = `
            <h3>Approval Required from ${hrName} HR Head</h3>
            <p>Employee: ${requesterName} (${empId})</p>
            <p>Request ID: <strong>${requestId}</strong></p>
            <p>Approved by FH: ${approverName}</p>
            <p>Please review and approve this request.</p>
        `;

        await apiInstance.sendTransacEmail(requesterEmailObj);
        await apiInstance.sendTransacEmail(hrEmailObj);

        res.status(200).json({ 
            success: true, 
            message: 'FH approval emails sent successfully',
            requestId: requestId
        });

    } catch (error) {
        console.error('[FH APPROVAL] ❌ Error:', error.message);
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send FH approval emails',
            details: error.message 
        });
    }
});

//email notication of Rejection of approval by FH to requester
app.post('/send-fh-rejection-email', async (req, res) => {
    console.log(`[FH Rejection] Endpoint hit at ${new Date().toISOString()}`);
    console.log(`Sending mail to Requester on rejection by FH`);
    console.log(`[EMAIL NOTIFY] ✅ Requester email sent to: ${requesterEmail}`);
    
    const { 
        requesterEmail,
        requesterName,
        approverEmail,
        approverName,
        requestId,
        empId,
        rejectionReason
    } = req.body;

    try {
        // Email to Requester ONLY
        const emailObj = new SibApiV3Sdk.SendSmtpEmail();
        emailObj.to = [{ email: requesterEmail }];
        emailObj.subject = `Ride Request #${requestId} Rejected`;
        emailObj.htmlContent = `
            <h3>Request Rejected by Functional Head</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab request (ID: ${requestId}) has been rejected by ${approverName}.</p>
            <p><strong>Reason:</strong> ${rejectionReason}</p>
        `;

        await apiInstance.sendTransacEmail(emailObj);
        res.status(200).json({ success: true });
    } catch (error) {
        console.error('Rejection email error:', error);
        res.status(500).json({ error: 'Email sending failed' });
    }
});
// HR Approval Endpoint (uses vendorEmail and vendorName from request body)
app.post('/send-hr-approval-email', async (req, res) => {
    console.log(`[HR APPROVAL] Endpoint hit at ${new Date().toISOString()}`);
    console.log(`Sending mail to Requester , FH and Vendor on approval by FH`);
    console.log(`[EMAIL NOTIFY] ✅ Requester email sent to: ${requesterEmail}`);
    console.log(`[EMAIL NOTIFY] ✅ FH email sent to: ${fhApproverEmail}`);
    console.log(`[EMAIL NOTIFY] ✅ vendor email sent to: ${vendorEmail}`);
    
    const { 
        requesterEmail,
        requesterName,
        approverEmail,
        approverName,
        requestId,
        empId,
        fhApproverName,
        fhApproverEmail,
        vendorEmail,
        vendorName
    } = req.body;

    if (!requesterEmail || !requestId || !vendorEmail || !vendorName) {
        return res.status(400).json({ error: 'Missing required fields' });
    }

    try {
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();

        // Email to Requester
        const requesterEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        requesterEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        requesterEmailObj.to = [{ email: requesterEmail }];
        requesterEmailObj.subject = `🎉 Ride Request #${requestId} - APPROVED!`;
        requesterEmailObj.htmlContent = `
            <h3>Final Approval Received!</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab request (ID: ${requestId}) has been fully approved and sent to the vendor.</p>
            <p>The vendor (${vendorName}) will contact you soon.</p>
        `;

        // Email to FH
        const fhEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        fhEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        fhEmailObj.to = [{ email: fhApproverEmail }];
        fhEmailObj.subject = `Ride Request #${requestId} Approved by HR`;
        fhEmailObj.htmlContent = `
            <h3>HR Approved Your Request</h3>
            <p>Hello ${fhApproverName},</p>
            <p>The request you approved (ID: ${requestId}) has been finalized by HR.</p>
            <p>Vendor: ${vendorName} (${vendorEmail})</p>
        `;

        // Email to Vendor
        const vendorEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        vendorEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        vendorEmailObj.to = [{ email: vendorEmail }];
        vendorEmailObj.subject = `🚕 New Cab Assignment - Request #${requestId}`;
        vendorEmailObj.htmlContent = `
            <h3>Cab Assignment Required</h3>
            <p><strong>Request ID:</strong> ${requestId}</p>
            <p><strong>Employee:</strong> ${requesterName} (${empId})</p>
            <p>This request has been approved by FH (${fhApproverName}) and HR (${approverName}).</p>
        `;

        await Promise.all([
            apiInstance.sendTransacEmail(requesterEmailObj),
            apiInstance.sendTransacEmail(fhEmailObj),
            apiInstance.sendTransacEmail(vendorEmailObj)
        ]);

        res.status(200).json({ 
            success: true, 
            message: 'HR approval emails sent successfully',
            requestId: requestId
        });

    } catch (error) {
        console.error('[HR APPROVAL] ❌ Error:', error.message);
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send HR approval emails',
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

// Fixed catch-all route with named parameter
app.use('/*all', (req, res) => {
    console.log(`[SERVER] Unhandled request: ${req.method} ${req.originalUrl}`);
    res.status(404).json({ error: 'Endpoint not found' });
});

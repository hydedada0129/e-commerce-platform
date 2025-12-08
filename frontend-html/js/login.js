$(document).ready(function(){
    const API_BASE_URL="http://localhost:8080";
    
    //parses string after '?' ex: http://localhost:8080/login.html?role=SELLER
    const params=new URLSearchParams(window.location.search);
    //get 'role' value
    const role=params.get('role');
    
    //selecting DOM elements
    //title&form
    const $pageTitle=$("#page-title");
    const $loginForm=$("#login-form");
    const $registerForm=$("#register-form");
    //container
    const $showRegisterLink=$("#show-register-link");
    const $showLoginLink=$("#show-login-link");
    const $switchContainer=$(".toggle-link");
    //
    const $messageContainer=$("#message-container");
//    const $sendCodeBtn=$("#show-login-link");
    const $sendCodeBtn = $("#send-code-btn");

    const $regEmailInput=$("#reg-email");
    const $regCodeInput=$("#reg-code");
    //message and Registration inputs
    const $loginAccountLabel=$("label[for='login-email']");
    const $loginAccountInput=$("#login-email");

    //=============
    //buyer, seller, admin selection
    //=============
    if(role==='BUYER'){
        $pageTitle.text("Buyer login/register");
    }else if(role==='SELLER'){
        $pageTitle.text("seller login/register");
    }else if(role==='ADMIN'){
        $pageTitle.text("admin login");
        $loginAccountLabel.text("admin number(Admin Code):");
        $loginAccountInput.attr("placeholder", "please enter admin code");

        if($switchContainer.length){
            $switchContainer.hide();
        }else if($showRegisterLink.length){
            $showRegisterLink.hide();
        }
    }else{
        window.location.href='index.html';
        return;
    }

    //===============
    //login or register
    //===============
    if(role!=='ADMIN'){
        $showRegisterLink.on("click", function(e){
            e.preventDefault();
            $loginForm.hide();
            $registerForm.show();
            showMessage('','clear');
        });

        $showLoginLink.on("click", function(e){
            e.preventDefault();
            $loginForm.show();
            $registerForm.hide();
            showMessage('','clear');
        });
    }

    //======================
    // Login
    //======================
    $loginForm.submit(function(e){
        e.preventDefault(); //it doesn't clear values

        //
        const accountInput=$loginAccountInput.val();    //email user entered
        const passwordInput=$loginForm.find("input[name='password']").val();
        const expectedRole=role;

        let loginUrl="";
        
        if(role==='ADMIN'){
            loginUrl=`${API_BASE_URL}/api/auth/admin-login`;
            payload={ adminCode: accountInput, password: passwordInput };
        }else{
            loginUrl=`${API_BASE_URL}/api/auth/login`;
            payload={ email: accountInput, password: passwordInput};
        }

        //send to server by ajax
        $.ajax({
            url:loginUrl,
            method:'POST',
            contentType: 'application/json',    //send by
            data:JSON.stringify(payload),       //converts to JSON String 

            //if Http connection is success(return 200), run callback function
            success:function(data){     //data => server return data by JSON 
                const actualRole=data.role;

                if(actualRole!==expectedRole){
                    showMessage(
                        `login falied: your account is ${getRoleName(actualRole)}, only for ${getRoleName(expectedRole)} to login `,
                        'error'
                    );
                    return;
                }

                //save token , role in user's PC
                localStorage.setItem('token', data.token);
                localStorage.setItem('role', actualRole);

                //admin 
                if(actualRole==='ADMIN'){
                    if(data.adminCode) localStorage.setItem('adminCode', data.adminCode);
                    if(data.name) localStorage.setItem('userName', data.name);
                }else{
                    //buyer, seller doesn't need adminCode
                    localStorage.removeItem('adminCode');
                }

                //show message and delay 1.5 second for forwarding
                showMessage('success login! forwarding... ', 'success');
                setTimeout(()=>{
                    if(actualRole==='BUYER') window.location.href = 'products.html';
                    if(actualRole==='SELLER') window.location.href='seller-dashboard.html';
                    if(actualRole==='ADMIN') window.location.href='admin-dashboard.html';
                }, 1500);
            },

            //if status code is not 200 from server: 
            //XMLHttpRequest(XHR) : async to server
            error: function(xhr){
                const erroMsg=xhr.responseText || "login failed";
                showMessage(errorMsg, 'error');
            }
        }); 
    })
    
    // ==========================================
    // 4. send validation code to user's email
    // ==========================================
    $sendCodeBtn.click(function(){
        //if ADMIN, exit the function
        if(role==='ADMIN') return;  

        //if no email entered, don't send
        const email=$regEmailInput.val();
        if(!email){
            showMessage("please enter your email", 'error');
            return;
        }
        
        //disable button before sent
        $sendCodeBtn.prop('disabled', true);

        let countdown=60;
        $sendCodeBtn.text(`resend(${countdown})`);
        //
        const timer=setInterval(()=>{
            countdown--;
            $sendCodeBtn.text(`resend(${countdown})`);
            if(countdown<=0){
                clearInterval(timer);
                $sendCodeBtn.prop("disabled", false).text("get validation code");
            }
        }, 1000);

        $.ajax({
            url:`${API_BASE_URL}/api/auth/send-code`,
            type:"POST",
            contentType:"application/json",
            data:JSON.stringify({email}),

            success:function(msg){
                showMessage(msg,"success");
            },

            error:function(xhr){
                clearInterval(timer);
                $sendCodeBtn.prop('disabled', false).text('get validation code');

                let message=xhr.responseJSON?.message||xhr.responseText||"send validation code failed";
                showMessage(message, 'error');
            }
        });
    });

    // ==========================================
    // 5. register
    // ==========================================
    $registerForm.submit(function(e){
        e.preventDefault();

        //if user is ADMIN, exit the function
        if(role==="ADMIN") return;

        //form data
        const formData={
            email:$regEmailInput.val(),
            password: $registerForm.find("input[name='password']").val(),
            name:$registerForm.find("input[name='name']").val(),
            phone:$registerForm.find("input[name='phone']").val(),
            address:$registerForm.find("input[name='address']").val(),
            code:$regCodeInput.val()
        };

        const registerUrl=`${API_BASE_URL}/api/auth/register/${role.toLowerCase()}`;

        //post form to server
        $.ajax({
            url:registerUrl,
            type:"POST",
            contentType:"application/json",
            data:JSON.stringify(formData),

            success:function(data){ //data: response from server
                //save token, role in local storage
                localStorage.setItem('token', data.token);
                localStorage.setItem('role', data.role);

                showMessage("registration successful! automatic logging in", "success");

                //redirect to other URL by role after 1.5 second timeout
                setTimeout(()=>{
                    if(data.role==='BUYER') window.location.href='products.html';
                    else if(data.role==='SELLER') window.location.href('seller-dashboard.html');
                }, 1500);
            },

            error: function(xhr){
                let msg=xhr.responseJSON?.message || xhr.responseText || "registration failed";
                showMessage(msg, 'error');
            }
        });
    });

    // ==========================================
    // other function
    // ==========================================

    //show message 
    function showMessage(msg, type){
        //if type is clear, hide "message-container"
        if(type==='clear'){
            $messageContainer.hide();
            return;
        }

        $messageContainer
            .removeClass()
            .addClass("message " + type)
            .text(msg)
            .show();
    }

    //get role's name in Chinese
    function getRoleName(roleCode){
        switch(roleCode){
            case "BUYER": return "買家";
            case "SELLER": return "賣家";
            case "ADMIN": return "管理員";
            default: return roleCode;
        }
    }
});
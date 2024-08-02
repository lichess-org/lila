import { DrawShape } from 'chessground/draw';

// Safari specifically requires globally unique IDs for SVG elements, even between different SVGs,
// so we concatenate a unique key to every ID for each SVG.
const svg = (key: string) => `
<g class="apple" transform-origin="50 50">
    <g transform="scale(4.15 4.15)">
        <g>
            <path d="M12.52 3.06287L15.1485 8.3943C15.1828 8.46858 15.2571 8.52001 15.3371 8.53144L21.2171 9.38858C21.6914 9.45715 21.88 10.04 21.5371 10.3772L17.4057 14.4C17.2685 14.5314 17.2057 14.7257 17.24 14.9143L18.2171 20.6C18.2971 21.0743 17.8 21.4343 17.3771 21.2114L12.1143 18.4457C12.04 18.4057 11.9543 18.4057 11.88 18.4457L6.61712 21.2114C6.19426 21.4343 5.69712 21.0743 5.77712 20.6L6.78855 14.7429C6.79998 14.6629 6.77712 14.5772 6.71426 14.52L2.46283 10.3714C2.11998 10.0343 2.30855 9.45144 2.78283 9.38287L8.66283 8.52572C8.74283 8.5143 8.81712 8.46287 8.85141 8.38858L11.48 3.05715C11.6914 2.6343 12.3085 2.6343 12.52 3.06287Z" fill="url(#paint0_radial${key})"></path>
            <path d="M5.54855 12.1828C4.44569 11.08 2.88569 9.82283 3.21141 9.32568L2.78283 9.38854C2.30855 9.45711 2.11998 10.04 2.46283 10.3771L6.71998 14.5257C6.77712 14.5828 6.80569 14.6685 6.79426 14.7485C6.94283 13.7085 6.6514 13.28 5.54855 12.1828Z" fill="url(#paint1_radial${key})"></path>
            <path d="M18.4514 12.1828C19.5543 11.08 21.1143 9.82283 20.7885 9.32568L21.2171 9.38854C21.6914 9.45711 21.88 10.04 21.5371 10.3771L17.28 14.5257C17.2228 14.5828 17.1943 14.6685 17.2057 14.7485C17.0571 13.7085 17.3485 13.28 18.4514 12.1828Z" fill="url(#paint2_radial${key})"></path>
            <path d="M21.2173 9.3887L15.3373 8.53156C15.2573 8.52013 15.183 8.4687 15.1487 8.39442L12.5201 3.06299C12.5201 3.06299 12.2801 3.36013 12.823 4.49156C13.1773 5.2287 14.1087 7.26299 14.6173 8.31442C14.903 8.9087 15.3373 8.92013 15.9944 9.00585L20.3373 9.56013C20.3316 9.56585 20.8116 9.60585 21.2173 9.3887Z" fill="url(#paint3_radial${key})"></path>
            <path d="M12.52 3.06287L15.1485 8.3943C15.1828 8.46858 15.2571 8.52001 15.3371 8.53144L21.2171 9.38858C21.6914 9.45715 21.88 10.04 21.5371 10.3772L17.4057 14.4C17.2685 14.5314 17.2057 14.7257 17.24 14.9143L18.2171 20.6C18.2971 21.0743 17.8 21.4343 17.3771 21.2114L12.1143 18.4457C12.04 18.4057 11.9543 18.4057 11.88 18.4457L6.61712 21.2114C6.19426 21.4343 5.69712 21.0743 5.77712 20.6L6.78855 14.7429C6.79998 14.6629 6.77712 14.5772 6.71426 14.52L2.46283 10.3714C2.11998 10.0343 2.30855 9.45144 2.78283 9.38287L8.66283 8.52572C8.74283 8.5143 8.81712 8.46287 8.85141 8.38858L11.48 3.05715C11.6914 2.6343 12.3085 2.6343 12.52 3.06287Z" fill="url(#paint4_radial${key})"></path>
            <path d="M12.52 3.06287L15.1485 8.3943C15.1828 8.46858 15.2571 8.52001 15.3371 8.53144L21.2171 9.38858C21.6914 9.45715 21.88 10.04 21.5371 10.3772L17.4057 14.4C17.2685 14.5314 17.2057 14.7257 17.24 14.9143L18.2171 20.6C18.2971 21.0743 17.8 21.4343 17.3771 21.2114L12.1143 18.4457C12.04 18.4057 11.9543 18.4057 11.88 18.4457L6.61712 21.2114C6.19426 21.4343 5.69712 21.0743 5.77712 20.6L6.78855 14.7429C6.79998 14.6629 6.77712 14.5772 6.71426 14.52L2.46283 10.3714C2.11998 10.0343 2.30855 9.45144 2.78283 9.38287L8.66283 8.52572C8.74283 8.5143 8.81712 8.46287 8.85141 8.38858L11.48 3.05715C11.6914 2.6343 12.3085 2.6343 12.52 3.06287Z" fill="url(#paint5_radial${key})"></path>
            <path d="M11.48 3.06287L8.85141 8.3943C8.81712 8.46858 8.74283 8.52001 8.66283 8.53144L2.78283 9.38858C2.30855 9.45715 2.11998 10.04 2.46283 10.3772L6.59426 14.4C6.73141 14.5314 6.79426 14.7257 6.75998 14.9143L5.78283 20.6C5.70283 21.0743 6.19998 21.4343 6.62283 21.2114L11.8857 18.4457C11.96 18.4057 12.0457 18.4057 12.12 18.4457L17.3828 21.2114C17.8057 21.4343 18.3028 21.0743 18.2228 20.6L17.2114 14.7429C17.2 14.6629 17.2228 14.5772 17.2857 14.52L21.5428 10.3714C21.8857 10.0343 21.6971 9.45144 21.2228 9.38287L15.3428 8.52572C15.2628 8.5143 15.1885 8.46287 15.1543 8.38858L12.5257 3.05715C12.3085 2.6343 11.6914 2.6343 11.48 3.06287Z" fill="url(#paint6_radial${key})"></path>
            <path d="M11.48 3.06287L8.85141 8.3943C8.81712 8.46858 8.74283 8.52001 8.66283 8.53144L2.78283 9.38858C2.30855 9.45715 2.11998 10.04 2.46283 10.3772L6.59426 14.4C6.73141 14.5314 6.79426 14.7257 6.75998 14.9143L5.78283 20.6C5.70283 21.0743 6.19998 21.4343 6.62283 21.2114L11.8857 18.4457C11.96 18.4057 12.0457 18.4057 12.12 18.4457L17.3828 21.2114C17.8057 21.4343 18.3028 21.0743 18.2228 20.6L17.2114 14.7429C17.2 14.6629 17.2228 14.5772 17.2857 14.52L21.5428 10.3714C21.8857 10.0343 21.6971 9.45144 21.2228 9.38287L15.3428 8.52572C15.2628 8.5143 15.1885 8.46287 15.1543 8.38858L12.5257 3.05715C12.3085 2.6343 11.6914 2.6343 11.48 3.06287Z" fill="url(#paint7_radial${key})"></path>
            <path opacity="0.24" d="M11.48 3.06287L8.85141 8.3943C8.81712 8.46858 8.74283 8.52001 8.66283 8.53144L2.78283 9.38858C2.30855 9.45715 2.11998 10.04 2.46283 10.3772L6.59426 14.4C6.73141 14.5314 6.79426 14.7257 6.75998 14.9143L5.78283 20.6C5.70283 21.0743 6.19998 21.4343 6.62283 21.2114L11.8857 18.4457C11.96 18.4057 12.0457 18.4057 12.12 18.4457L17.3828 21.2114C17.8057 21.4343 18.3028 21.0743 18.2228 20.6L17.2114 14.7429C17.2 14.6629 17.2228 14.5772 17.2857 14.52L21.5428 10.3714C21.8857 10.0343 21.6971 9.45144 21.2228 9.38287L15.3428 8.52572C15.2628 8.5143 15.1885 8.46287 15.1543 8.38858L12.5257 3.05715C12.3085 2.6343 11.6914 2.6343 11.48 3.06287Z" fill="url(#paint8_radial${key})"></path>
            <path opacity="0.24" d="M11.48 3.06287L8.85141 8.3943C8.81712 8.46858 8.74283 8.52001 8.66283 8.53144L2.78283 9.38858C2.30855 9.45715 2.11998 10.04 2.46283 10.3772L6.59426 14.4C6.73141 14.5314 6.79426 14.7257 6.75998 14.9143L5.78283 20.6C5.70283 21.0743 6.19998 21.4343 6.62283 21.2114L11.8857 18.4457C11.96 18.4057 12.0457 18.4057 12.12 18.4457L17.3828 21.2114C17.8057 21.4343 18.3028 21.0743 18.2228 20.6L17.2114 14.7429C17.2 14.6629 17.2228 14.5772 17.2857 14.52L21.5428 10.3714C21.8857 10.0343 21.6971 9.45144 21.2228 9.38287L15.3428 8.52572C15.2628 8.5143 15.1885 8.46287 15.1543 8.38858L12.5257 3.05715C12.3085 2.6343 11.6914 2.6343 11.48 3.06287Z" fill="url(#paint9_radial${key})"></path>
            <path d="M2.78271 9.3887L8.66272 8.53156C8.74272 8.52013 8.817 8.4687 8.85129 8.39442L11.4799 3.06299C11.4799 3.06299 11.7199 3.36013 11.177 4.49156C10.8227 5.2287 9.89129 7.26299 9.38272 8.31442C9.097 8.9087 8.66272 8.92013 8.00557 9.00585L3.66271 9.56013C3.66843 9.56585 3.18843 9.60585 2.78271 9.3887Z" fill="url(#paint10_radial${key})"></path>
            <path opacity="0.5" d="M12.52 3.06287L15.1485 8.3943C15.1828 8.46858 15.2571 8.52001 15.3371 8.53144L21.2171 9.38858C21.6914 9.45715 21.88 10.04 21.5371 10.3772L17.4057 14.4C17.2685 14.5314 17.2057 14.7257 17.24 14.9143L18.2171 20.6C18.2971 21.0743 17.8 21.4343 17.3771 21.2114L12.1143 18.4457C12.04 18.4057 11.9543 18.4057 11.88 18.4457L6.61712 21.2114C6.19426 21.4343 5.69712 21.0743 5.77712 20.6L6.78855 14.7429C6.79998 14.6629 6.77712 14.5772 6.71426 14.52L2.46283 10.3714C2.11998 10.0343 2.30855 9.45144 2.78283 9.38287L8.66283 8.52572C8.74283 8.5143 8.81712 8.46287 8.85141 8.38858L11.48 3.05715C11.6914 2.6343 12.3085 2.6343 12.52 3.06287Z" fill="url(#paint11_radial${key})"></path>
            <path d="M18.2171 20.6001L17.24 14.9144C17.2057 14.7258 17.2685 14.5373 17.4057 14.4001L21.5371 10.3716C21.88 10.0344 21.6914 9.45155 21.2171 9.38298L15.3371 8.52584C15.2571 8.51441 15.1828 8.46298 15.1485 8.3887L12.52 3.05727C12.3085 2.6287 11.6971 2.6287 11.48 3.05727L8.85141 8.3887C8.81712 8.46298 8.74283 8.51441 8.66283 8.52584L2.78283 9.38298C2.30855 9.45155 2.11998 10.0344 2.46283 10.3716L6.71998 14.5201C6.77712 14.5773 6.80569 14.663 6.79426 14.743L5.78283 20.6001C5.70283 21.0744 6.19998 21.4344 6.62283 21.2116L11.8857 18.4458C11.96 18.4058 12.0457 18.4058 12.12 18.4458L17.3828 21.2116C17.8 21.4344 18.2971 21.0744 18.2171 20.6001ZM17.7943 20.8973C17.7543 20.9258 17.6571 20.983 17.5314 20.9144L12.2743 18.1487C12.1885 18.103 12.0971 18.0801 12.0057 18.0801C11.9143 18.0801 11.8228 18.103 11.7371 18.1487L6.47998 20.9144C6.35426 20.9773 6.25712 20.9201 6.21712 20.8973C6.17712 20.8687 6.09141 20.7887 6.11998 20.6516L7.11426 14.8001C7.14855 14.6116 7.08569 14.423 6.94855 14.2858L2.69141 10.1373C2.58855 10.0401 2.61141 9.92584 2.62855 9.88013C2.64569 9.83441 2.69141 9.73155 2.82855 9.7087L8.70855 8.85155C8.89712 8.82298 9.06283 8.70298 9.14283 8.53727L11.7714 3.20584C11.8343 3.08013 11.9485 3.0687 11.9943 3.0687C12.04 3.0687 12.1543 3.08013 12.2171 3.20584L14.8457 8.53727C14.9314 8.7087 15.0914 8.8287 15.28 8.85155L21.16 9.7087C21.2971 9.73155 21.3485 9.83441 21.36 9.88013C21.3771 9.92584 21.4 10.0401 21.2971 10.1373L17.1657 14.1658C16.9543 14.3716 16.8571 14.6744 16.9028 14.9658L17.88 20.6516C17.9143 20.7944 17.8285 20.8744 17.7943 20.8973Z" fill="url(#paint12_radial${key})"></path>
            <defs>
                <radialGradient id="paint0_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(11.5169 10.0777) scale(9.49336)">
                    <stop stop-color="#FFE343"></stop>
                    <stop offset="0.5492" stop-color="#FFE241"></stop>
                    <stop offset="0.7469" stop-color="#FFDF3A"></stop>
                    <stop offset="0.8874" stop-color="#FEDA2F"></stop>
                    <stop offset="1" stop-color="#FED31E"></stop>
                </radialGradient>
                <radialGradient id="paint1_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(4.49599 13.8245) rotate(-39.4857) scale(1.75579 4.72781)">
                    <stop stop-color="#D86D00"></stop>
                    <stop offset="0.3292" stop-color="#DC6C0A" stop-opacity="0.6708"></stop>
                    <stop offset="0.8792" stop-color="#E86823" stop-opacity="0.1208"></stop>
                    <stop offset="1" stop-color="#EB672A" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint2_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(19.2812 14.0812) rotate(-140.514) scale(1.75579 4.72781)">
                    <stop stop-color="#D86D00"></stop>
                    <stop offset="0.3292" stop-color="#DC6C0A" stop-opacity="0.6708"></stop>
                    <stop offset="0.8792" stop-color="#E86823" stop-opacity="0.1208"></stop>
                    <stop offset="1" stop-color="#EB672A" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint3_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(18.0285 4.69362) rotate(46.8275) scale(6.8756 4.73243)">
                    <stop stop-color="#ED9900"></stop>
                    <stop offset="1" stop-color="#ED9900" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint4_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(11.2624 9.07147) rotate(-41.4123) scale(5.46203 2.99319)">
                    <stop stop-color="#FFEC5F"></stop>
                    <stop offset="1" stop-color="#FFEC5F" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint5_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(9.62409 21.2697) rotate(-26.3216) scale(5.21989 2.47385)">
                    <stop stop-color="#D86D00"></stop>
                    <stop offset="0.3292" stop-color="#DC6C0A" stop-opacity="0.6708"></stop>
                    <stop offset="0.8792" stop-color="#E86823" stop-opacity="0.1208"></stop>
                    <stop offset="1" stop-color="#EB672A" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint6_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(14.3179 21.4168) rotate(-153.678) scale(5.21989 2.47385)">
                    <stop stop-color="#D86D00"></stop>
                    <stop offset="0.3292" stop-color="#DC6C0A" stop-opacity="0.6708"></stop>
                    <stop offset="0.8792" stop-color="#E86823" stop-opacity="0.1208"></stop>
                    <stop offset="1" stop-color="#EB672A" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint7_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(11.9948 24.1965) rotate(177.226) scale(6.90514 7.07936)">
                    <stop stop-color="#D86D00"></stop>
                    <stop offset="0.3292" stop-color="#DC6C0A" stop-opacity="0.6708"></stop>
                    <stop offset="0.8792" stop-color="#E86823" stop-opacity="0.1208"></stop>
                    <stop offset="1" stop-color="#EB672A" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint8_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(14.3253 16.556) rotate(158.456) scale(7.29673 5.51594)">
                    <stop stop-color="#D86D00"></stop>
                    <stop offset="0.3292" stop-color="#DC6C0A" stop-opacity="0.6708"></stop>
                    <stop offset="0.8792" stop-color="#E86823" stop-opacity="0.1208"></stop>
                    <stop offset="1" stop-color="#EB672A" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint9_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(11.2388 9.84634) rotate(158.456) scale(4.39634 3.3234)">
                    <stop stop-color="white"></stop>
                    <stop offset="1" stop-color="white" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint10_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(5.97178 4.69288) rotate(133.173) scale(6.8756 4.73243)">
                    <stop stop-color="#ED9900"></stop>
                    <stop offset="1" stop-color="#ED9900" stop-opacity="0"></stop>
                </radialGradient>
                <radialGradient id="paint11_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(12.1073 12.4931) scale(11.3187)">
                    <stop stop-color="#FF8000" stop-opacity="0"></stop>
                    <stop offset="0.5434" stop-color="#FD7F00" stop-opacity="0.5434"></stop>
                    <stop offset="0.7391" stop-color="#F67C00" stop-opacity="0.7391"></stop>
                    <stop offset="0.8781" stop-color="#EB7600" stop-opacity="0.8781"></stop>
                    <stop offset="0.9903" stop-color="#DA6E00" stop-opacity="0.9903"></stop>
                    <stop offset="1" stop-color="#D86D00"></stop>
                </radialGradient>
                <radialGradient id="paint12_radial${key}" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(12 12.8584) scale(9.78074)">
                    <stop stop-color="#A3541E" stop-opacity="0.5"></stop>
                    <stop offset="0.5109" stop-color="#A5551D" stop-opacity="0.7555"></stop>
                    <stop offset="0.695" stop-color="#AC5819" stop-opacity="0.8475"></stop>
                    <stop offset="0.8261" stop-color="#B75E12" stop-opacity="0.9131"></stop>
                    <stop offset="0.9315" stop-color="#C86609" stop-opacity="0.9657"></stop>
                    <stop offset="1" stop-color="#D86D00"></stop>
                </radialGradient>
            </defs>
        </g>
    </g>
</g>`;

export const makeAppleShape = (key: Key): DrawShape => ({
  orig: key,
  customSvg: {
    html: svg(key),
  },
});
